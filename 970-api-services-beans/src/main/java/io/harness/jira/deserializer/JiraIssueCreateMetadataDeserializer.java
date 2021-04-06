package io.harness.jira.deserializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.JiraIssueCreateMetadataNG;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(CDC)
public class JiraIssueCreateMetadataDeserializer extends StdDeserializer<JiraIssueCreateMetadataNG> {
  public JiraIssueCreateMetadataDeserializer() {
    this(null);
  }

  public JiraIssueCreateMetadataDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JiraIssueCreateMetadataNG deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new JiraIssueCreateMetadataNG(node);
  }
}