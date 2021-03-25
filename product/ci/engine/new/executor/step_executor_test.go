package executor

import (
	"context"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	statuspb "github.com/wings-software/portal/910-delegate-task-grpc-service/src/main/proto/io/harness/task/service"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestStepValidations(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	callbackToken := "token"
	taskID := "taskID"
	tmpFilePath := "/tmp"

	stepProto1 := &pb.UnitStep{
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{},
		},
	}
	stepProto3 := &pb.UnitStep{
		Id: "test1",
	}

	stepProto4 := &pb.UnitStep{
		Id:            "test1",
		CallbackToken: callbackToken,
	}

	stepProto5 := &pb.UnitStep{
		Id:            "test1",
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	tests := []struct {
		name        string
		step        *pb.UnitStep
		expectedErr bool
	}{
		{
			name:        "step ID is not set",
			step:        stepProto1,
			expectedErr: true,
		},
		{
			name:        "callback token is not set",
			step:        stepProto3,
			expectedErr: true,
		},
		{
			name:        "task ID is not set",
			step:        stepProto4,
			expectedErr: true,
		},
		{
			name:        "account ID is not set",
			step:        stepProto5,
			expectedErr: true,
		},
	}

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, stepID, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		status statuspb.StepExecutionStatus, errMsg string, stepOutput *output.StepOutput, log *zap.SugaredLogger) error {
		return nil
	}
	for _, tc := range tests {
		e := NewStepExecutor(tmpFilePath, log.Sugar())
		got := e.Run(ctx, tc.step)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func TestStepError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test2",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command:       "ls",
				ContainerPort: uint32(8000),
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
		AccountId:     accountID,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldAddonExecutor := executeStepOnAddon
	defer func() { executeStepOnAddon = oldAddonExecutor }()
	executeStepOnAddon = func(ctx context.Context, step *pb.UnitStep, tmpFilePath string,
		log *zap.SugaredLogger) (*output.StepOutput, error) {
		return nil, errors.New("failed")
	}

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, stepID, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		status statuspb.StepExecutionStatus, errMsg string, stepOutput *output.StepOutput, log *zap.SugaredLogger) error {
		return nil
	}

	e := NewStepExecutor(tmpFilePath, log.Sugar())
	err := e.Run(ctx, stepProto)
	assert.NotEqual(t, err, nil)
}

func TestStepRunSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test2",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command:       "ls",
				ContainerPort: uint32(8000),
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
		AccountId:     accountID,
	}

	outputKey := "foo"
	outputVal := "bar"

	o := &output.StepOutput{}
	o.Output.Variables = map[string]string{outputKey: outputVal}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldAddonExecutor := executeStepOnAddon
	defer func() { executeStepOnAddon = oldAddonExecutor }()
	executeStepOnAddon = func(ctx context.Context, step *pb.UnitStep, tmpFilePath string,
		log *zap.SugaredLogger) (*output.StepOutput, error) {
		return o, nil
	}

	oldStopAdddon := stopAddon
	defer func() { stopAddon = oldStopAdddon }()
	stopAddon = func(ctx context.Context, stepID string, port uint32,
		log *zap.SugaredLogger) error {
		return nil
	}

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, stepID, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		status statuspb.StepExecutionStatus, errMsg string, stepOutput *output.StepOutput, log *zap.SugaredLogger) error {
		return nil
	}

	e := NewStepExecutor(tmpFilePath, log.Sugar())
	err := e.Run(ctx, stepProto)
	assert.Equal(t, err, nil)
}