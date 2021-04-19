package io.harness.gitsync.entityInfo;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.EntityDetail;

import java.util.function.Supplier;

@OwnedBy(DX)
public interface GitSdkEntityHandlerInterface<B extends GitSyncableEntity, Y extends YamlDTO> {
  Supplier<Y> getYamlFromEntity(B entity);

  EntityType getEntityType();

  Supplier<B> getEntityFromYaml(Y yaml);

  EntityDetail getEntityDetail(B entity);

  Y save(Y yaml, String accountIdentifier);

  Y update(Y yaml, String accountIdentifier);

  boolean delete(EntityReference entityReference);
}