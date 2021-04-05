package io.harness.audit.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "YamlDiffRecordKeys")
@Entity(value = "yamlDiff", noClassnameStored = true)
@Document("yamlDiff")
@TypeAlias("yamlDiff")
@JsonInclude(NON_NULL)
@StoreIn(DbAliases.AUDITS)
public class YamlDiffRecord {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotNull String auditId;
  @NotNull String accountIdentifier;
  String oldYaml;
  String newYaml;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder().name("auditIdx").field(YamlDiffRecordKeys.auditId).build())
        .add(CompoundMongoIndex.builder()
                 .name("accountIdentifierIdx")
                 .field(YamlDiffRecordKeys.accountIdentifier)
                 .build())
        .build();
  }
}