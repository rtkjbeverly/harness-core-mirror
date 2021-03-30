package io.harness.gitsync;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.beans.SampleBean1;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.ng.core.EntityDetail;

import com.google.inject.Singleton;
import java.util.function.Supplier;

@Singleton
public class SampleBean1EntityGitPersistenceHelperServiceImpl
    implements EntityGitPersistenceHelperService<SampleBean1, SampleBean1> {
  @Override
  public EntityType getEntityType() {
    return EntityType.CONNECTORS;
  }

  @Override
  public Supplier<SampleBean1> getEntityFromYaml(SampleBean1 yaml) {
    return null;
  }

  @Override
  public EntityDetail getEntityDetail(SampleBean1 entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .entityRef(IdentifierRef.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .identifier(entity.getIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .build())
        .build();
  }
}