package io.harness.ng.userprofile.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "SCMKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "sourceCodeManagers", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("sourceCodeManagers")
@StoreIn(DbAliases.NG_MANAGER)
public abstract class SourceCodeManager implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("query_idx")
                 .unique(true)
                 .field(SCMKeys.userIdentifier)
                 .field(SCMKeys.name)
                 .build())
        .build();
  }

  @JsonIgnore @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty String userIdentifier;
  String name;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;

  public abstract SCMType getType();

  public abstract static class SourceCodeManagerMapper<D extends SourceCodeManagerDTO, B extends SourceCodeManager> {
    public abstract B toSCMEntity(D sourceCodeManagerDTO);

    public abstract D toSCMDTO(B sourceCodeManager);

    public void setCommonFieldsEntity(SourceCodeManager scm, SourceCodeManagerDTO scmDTO) {
      scm.setName(scmDTO.getName());
      scm.setUserIdentifier(scmDTO.getUserIdentifier());
    }

    public void setCommonFieldsDTO(SourceCodeManager scm, SourceCodeManagerDTO scmDTO) {
      scmDTO.setName(scm.getName());
      scmDTO.setUserIdentifier(scm.getUserIdentifier());
      scmDTO.setCreatedAt(scm.getCreatedAt());
      scmDTO.setLastModifiedAt(scm.getLastModifiedAt());
    }
  }
}