package io.harness.repositories.gittoharnessstatus;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessProgress;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface GitToHarnessProgressRepository
    extends CrudRepository<GitToHarnessProgress, String>, GitToHarnessProgressRepositoryCustom {}