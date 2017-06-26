package software.wings.integration;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static software.wings.beans.BambooConfig.Builder.aBambooConfig;
import static software.wings.beans.DockerConfig.Builder.aDockerConfig;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.ResponseMessage.Builder.aResponseMessage;
import static software.wings.beans.ResponseMessage.ResponseTypeEnum.ERROR;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.config.NexusConfig.Builder.aNexusConfig;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.rules.RepeatRule.Repeat;

import java.util.Arrays;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 5/3/17.
 */
public class SettingServiceIntegrationTest extends BaseIntegrationTest {
  private static final char[] JENKINS_PASSWORD = "admin".toCharArray();
  private static final String JENKINS_URL = "http://ec2-34-207-79-21.compute-1.amazonaws.com:8080/";
  private static final String JENKINS_USERNAME = "admin";
  private static final String NEXUS_URL = "https://nexus.wings.software";
  private static final String NEXUS_USERNAME = "admin";
  private static final char[] NEXUS_PASSWORD = "wings123!".toCharArray();
  private static final String BAMBOO_URL = "http://ec2-34-202-14-12.compute-1.amazonaws.com:8085/";
  private static final String BAMBOO_USERNAME = "wingsbuild";
  private static final char[] BAMBOO_PASSWORD = "0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray();
  private static final String DOCKER_REGISTRY_URL = "https://registry.hub.docker.com/v2/";
  private static final String DOCKER_USERNAME = "wingsplugins";
  private static final char[] DOCKER_PASSOWRD = "W!ngs@DockerHub".toCharArray();

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(SettingAttribute.class));
  }

  @Test
  @Repeat(times = 5)
  public void shouldSaveJenkinsConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(Entity.entity(aSettingAttribute()
                                    .withName("Wings Jenkins")
                                    .withCategory(Category.CONNECTOR)
                                    .withAccountId(accountId)
                                    .withValue(aJenkinsConfig()
                                                   .withAccountId(accountId)
                                                   .withJenkinsUrl(JENKINS_URL)
                                                   .withUsername(JENKINS_USERNAME)
                                                   .withPassword(JENKINS_PASSWORD)
                                                   .build())
                                    .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});

    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("jenkinsUrl", "username", "password", "accountId")
        .contains(tuple(JENKINS_URL, JENKINS_USERNAME, null, accountId));
  }

  @Test
  public void shouldThrowExceptionForUnreachableJenkinsUrl() {
    Response response = getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
                            .post(Entity.entity(aSettingAttribute()
                                                    .withName("Wings Jenkins")
                                                    .withCategory(Category.CONNECTOR)
                                                    .withAccountId(accountId)
                                                    .withValue(aJenkinsConfig()
                                                                   .withAccountId(accountId)
                                                                   .withJenkinsUrl("BAD_URL")
                                                                   .withUsername(JENKINS_USERNAME)
                                                                   .withPassword(JENKINS_PASSWORD)
                                                                   .build())
                                                    .build(),
                                APPLICATION_JSON));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(RestResponse.class).getResponseMessages())
        .containsExactly(aResponseMessage()
                             .withCode(ErrorCode.INVALID_ARTIFACT_SERVER)
                             .withMessage("Jenkins URL must be a valid URL")
                             .withErrorType(ERROR)
                             .build());
  }

  @Test
  public void shouldSaveNexusConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(Entity.entity(aSettingAttribute()
                                    .withName("Wings Nexus")
                                    .withCategory(Category.CONNECTOR)
                                    .withAccountId(accountId)
                                    .withValue(aNexusConfig()
                                                   .withNexusUrl(NEXUS_URL)
                                                   .withUsername(NEXUS_USERNAME)
                                                   .withPassword(NEXUS_PASSWORD)
                                                   .withAccountId(accountId)
                                                   .build())
                                    .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("nexusUrl", "username", "password")
        .contains(tuple(NEXUS_URL, NEXUS_USERNAME, null));
  }

  @Test
  public void shouldSaveBambooConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(Entity.entity(aSettingAttribute()
                                    .withName("Wings Bamboo")
                                    .withCategory(Category.CONNECTOR)
                                    .withAccountId(accountId)
                                    .withValue(aBambooConfig()
                                                   .withAccountId(accountId)
                                                   .withBambooUrl(BAMBOO_URL)
                                                   .withUsername(BAMBOO_USERNAME)
                                                   .withPassword(BAMBOO_PASSWORD)
                                                   .build())
                                    .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("bambooUrl", "username", "password", "accountId")
        .contains(tuple(BAMBOO_URL, BAMBOO_USERNAME, null, accountId));
  }

  @Test
  public void shouldSaveDockerConfig() {
    RestResponse<SettingAttribute> restResponse =
        getRequestBuilderWithAuthHeader(getListWebTarget(accountId))
            .post(Entity.entity(aSettingAttribute()
                                    .withName("Wings Docker Registry")
                                    .withCategory(Category.CONNECTOR)
                                    .withAccountId(accountId)
                                    .withValue(aDockerConfig()
                                                   .withAccountId(accountId)
                                                   .withDockerRegistryUrl(DOCKER_REGISTRY_URL)
                                                   .withUsername(DOCKER_USERNAME)
                                                   .withPassword(DOCKER_PASSOWRD)
                                                   .build())
                                    .build(),
                      APPLICATION_JSON),
                new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(restResponse.getResource())
        .isInstanceOf(SettingAttribute.class)
        .extracting("value")
        .extracting("dockerRegistryUrl", "username", "password", "accountId")
        .contains(tuple(DOCKER_REGISTRY_URL, DOCKER_USERNAME, null, accountId));
  }

  private WebTarget getListWebTarget(String accountId) {
    return client.target(String.format("%s/settings/?accountId=%s", API_BASE, accountId));
  }

  private WebTarget getEntityWebTarget(String accountId, String entityId) {
    return client.target(String.format("%s/settings/%s/?accountId=%s", API_BASE, entityId, accountId));
  }
}
