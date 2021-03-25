package executor

import (
	"context"
	"errors"
	"fmt"
	"time"

	statuspb "github.com/wings-software/portal/910-delegate-task-grpc-service/src/main/proto/io/harness/task/service"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/status"
	"go.uber.org/zap"
	grpcstatus "google.golang.org/grpc/status"
)

var (
	sendStepStatus     = status.SendStepStatus
	executeStepOnAddon = ExecuteStepOnAddon
	stopAddon          = StopAddon
)

//go:generate mockgen -source step_executor.go -package=executor -destination mocks/step_executor_mock.go StepExecutor

// StepExecutor represents an interface to execute a unit step
type StepExecutor interface {
	Run(ctx context.Context, step *pb.UnitStep) error
}

type stepExecutor struct {
	tmpFilePath string // File path to store generated temporary files
	log         *zap.SugaredLogger
}

// NewStepExecutor creates a unit step executor
func NewStepExecutor(tmpFilePath string, log *zap.SugaredLogger) StepExecutor {
	return &stepExecutor{
		tmpFilePath: tmpFilePath,
		log:         log,
	}
}

// Executes a unit step and returns whether step execution completed successfully or not.
func (e *stepExecutor) Run(ctx context.Context, step *pb.UnitStep) error {
	start := time.Now()
	e.log.Infow("Step info", "step", step.String(), "step_id", step.GetId())

	stepOutput, err := e.execute(ctx, step)
	// Stops the addon container if step executed successfully.
	// If step fails, then it can be retried on the same container.
	// Hence, not stopping failed step containers.
	if err == nil {
		stopAddon(ctx, step.GetId(), step.GetContainerPort(), e.log)
	}

	statusErr := e.updateStepStatus(ctx, step, stepOutput, err, time.Since(start))
	if statusErr != nil {
		return statusErr
	}

	return err
}

func (e *stepExecutor) validate(step *pb.UnitStep) error {
	if step.GetId() == "" {
		err := fmt.Errorf("Step ID should be non-empty")
		e.log.Errorw("Step ID is not set", zap.Error(err))
		return err
	}
	if step.GetCallbackToken() == "" {
		err := fmt.Errorf("Callback token should be non-empty")
		e.log.Errorw("Callback token is not set", "step_id", step.GetId(),
			zap.Error(err))
		return err
	}
	if step.GetTaskId() == "" {
		err := fmt.Errorf("Task ID should be non-empty")
		e.log.Errorw("Task ID is not set", "step_id", step.GetId(),
			zap.Error(err))
		return err
	}
	if step.GetAccountId() == "" {
		err := fmt.Errorf("Account ID should be non-empty")
		e.log.Errorw("Account ID is not set", "step_id", step.GetId(),
			zap.Error(err))
		return err
	}
	return nil
}

func (e *stepExecutor) execute(ctx context.Context, step *pb.UnitStep) (
	*output.StepOutput, error) {
	if err := e.validate(step); err != nil {
		return nil, err
	}

	return executeStepOnAddon(ctx, step, e.tmpFilePath, e.log)
}

func (e *stepExecutor) updateStepStatus(ctx context.Context, step *pb.UnitStep,
	so *output.StepOutput, stepErr error, timeTaken time.Duration) error {
	callbackToken := step.GetCallbackToken()
	taskID := step.GetTaskId()
	stepID := step.GetId()
	accountID := step.GetAccountId()

	stepStatus := statuspb.StepExecutionStatus_SUCCESS
	errMsg := ""
	if stepErr != nil {
		stepStatus = statuspb.StepExecutionStatus_FAILURE
		if errors.Is(stepErr, context.DeadlineExceeded) {
			stepStatus = statuspb.StepExecutionStatus_ABORTED
		}

		errMsg = stepErr.Error()
		if e, ok := grpcstatus.FromError(stepErr); ok {
			errMsg = e.Message()
		}
	}

	err := sendStepStatus(ctx, stepID, accountID, callbackToken, taskID, int32(1),
		timeTaken, stepStatus, errMsg, so, e.log)
	if err != nil {
		e.log.Errorw("Failed to send step status. Failing execution of step",
			"step_id", stepID, zap.Error(err))
		return err
	}
	return nil
}