package io.harness.connector.mappers.newrelicmapper;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.newerlicmapper.NewRelicEntityToDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class NewRelicEntityToDTOTest extends CategoryTest {
  @InjectMocks private NewRelicEntityToDTO newRelicEntityToDTO;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testConvertEntitiyToDto() {
    String secretIdentifier = "secretIdentifier";
    String apiKeyRef = "apiKeyRef";
    String accountId = "accountId";
    String url = "https://insights-api.newrelic.com";

    NewRelicConnector connector = NewRelicConnector.builder().newRelicAccountId(accountId).apiKeyRef(apiKeyRef).build();

    NewRelicConnectorDTO connectorDTO = newRelicEntityToDTO.createConnectorDTO(connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getNewRelicAccountId()).isEqualTo(connector.getNewRelicAccountId());
    assertThat(connector.getUrl()).isEqualTo(connectorDTO.getUrl());
    assertThat(connectorDTO.getApiKeyRef().getIdentifier()).isEqualTo(connector.getApiKeyRef());
  }
}