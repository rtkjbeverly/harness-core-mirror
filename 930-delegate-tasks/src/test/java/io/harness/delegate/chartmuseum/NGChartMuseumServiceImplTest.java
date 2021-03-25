package io.harness.delegate.chartmuseum;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.chartmuseum.ChartMuseumClientHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NGChartMuseumServiceImplTest extends CategoryTest {
  @Mock private ChartMuseumClientHelper clientHelper;
  @InjectMocks private NGChartMuseumServiceImpl ngChartMuseumService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartMuseumServerUsingAwsS3InheritFromDelegate() throws Exception {
    testStartChartMuseumServerUsingAwsS3(AwsCredentialDTO.builder()
                                             .awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                                             .config(AwsInheritFromDelegateSpecDTO.builder().build())
                                             .build(),
        true, null, null);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStartChartMuseumServerUsingAwsS3ManualCredentials() throws Exception {
    final char[] accessKey = "access-key".toCharArray();
    final char[] secretKey = "secret-key".toCharArray();

    testStartChartMuseumServerUsingAwsS3(
        AwsCredentialDTO.builder()
            .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
            .config(AwsManualConfigSpecDTO.builder()
                        .accessKeyRef(SecretRefData.builder().decryptedValue(accessKey).build())
                        .secretKeyRef(SecretRefData.builder().decryptedValue(secretKey).build())
                        .build())
            .build(),
        false, accessKey, secretKey);
  }

  private void testStartChartMuseumServerUsingAwsS3(
      AwsCredentialDTO credentials, boolean inheritFromDelegate, char[] accessKey, char[] secretKey) throws Exception {
    final String bucketName = "bucketName";
    final String region = "region";
    final String folderPath = "folderPath";

    S3HelmStoreDelegateConfig s3StoreDelegateConfig =
        S3HelmStoreDelegateConfig.builder()
            .bucketName(bucketName)
            .region(region)
            .folderPath(folderPath)
            .awsConnector(AwsConnectorDTO.builder().credential(credentials).build())
            .build();

    ngChartMuseumService.startChartMuseumServer(s3StoreDelegateConfig, "resources");
    verify(clientHelper, times(1))
        .startS3ChartMuseumServer(bucketName, folderPath, region, inheritFromDelegate, accessKey, secretKey);
  }
}