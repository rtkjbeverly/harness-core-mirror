package software.wings.graphql.datafetcher.service;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.api.DeploymentType;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.service.QLDeploymentType;
import software.wings.graphql.schema.type.aggregation.service.QLDeploymentTypeFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceEntityAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilter;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceStatsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject ServiceStatsDataFetcher serviceStatsDataFetcher;
  String[] array = new String[1];
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void testServiceStatsDataFetcher() {
    Service service = createService(ACCOUNT1_ID, APP1_ID_ACCOUNT1, SERVICE1_ID_APP1_ACCOUNT1, SERVICE1_ID_APP1_ACCOUNT1,
        TAG_MODULE, TAG_VALUE_MODULE1, DeploymentType.KUBERNETES);
    array[0] = service.getUuid();
    QLIdFilter idFilter = QLIdFilter.builder().operator(QLIdOperator.IN).values(array).build();
    QLServiceFilter qlServiceFilter =
        QLServiceFilter.builder()
            .service(idFilter)
            .application(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {APP1_ID_ACCOUNT1}).build())
            .deploymentType(QLDeploymentTypeFilter.builder()
                                .operator(QLEnumOperator.IN)
                                .values(new QLDeploymentType[] {QLDeploymentType.KUBERNETES})
                                .build())
            .build();
    List<QLServiceFilter> serviceFilters = Arrays.asList(qlServiceFilter);
    QLServiceAggregation qlServiceAggregation =
        QLServiceAggregation.builder().entityAggregation(QLServiceEntityAggregation.DeploymentType).build();
    List<QLServiceAggregation> serviceAggregations = Arrays.asList(qlServiceAggregation);
    final QLData qlData =
        serviceStatsDataFetcher.fetch(ACCOUNT1_ID, null, serviceFilters, serviceAggregations, null, null);
    assertThat(qlData).isInstanceOf(QLAggregatedData.class);
    QLAggregatedData aggregatedData = (QLAggregatedData) qlData;
    assertThat(aggregatedData.getDataPoints().size()).isEqualTo(1);
  }
}