package software.wings.service.impl;

import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.BambooConfig;
import software.wings.beans.FileMetadata;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactPathServiceEntry;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.SettingsService;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/30/16.
 */
@Singleton
public class BambooArtifactCollectorServiceImpl implements ArtifactCollectorService {
  /**
   * The Settings service.
   */
  @Inject SettingsService settingsService;
  @Inject private FileService fileService;
  @Inject private BambooService bambooService;
  /**
   * The constant BUILD_NO.
   */
  public static final String BUILD_NO = "buildNo";

  @Override
  public List<ArtifactFile> collect(ArtifactStream artifactStream, Map<String, String> arguments) {
    List<ArtifactFile> artifactFiles = Lists.newArrayList();
    InputStream in = null;
    try {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();

      for (ArtifactPathServiceEntry artifactPathServiceEntry : artifactStream.getArtifactPathServices()) {
        Pair<String, InputStream> fileInfo = bambooService.downloadArtifact(bambooConfig, artifactStream.getJobname(),
            arguments.get(BUILD_NO), artifactPathServiceEntry.getArtifactPathRegex());
        if (fileInfo == null) {
          throw new FileNotFoundException(
              "Unable to get artifact from Bamboo for path " + artifactPathServiceEntry.getArtifactPathRegex());
        }
        in = fileInfo.getValue();
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(fileInfo.getKey());
        String uuid = fileService.saveFile(fileMetadata, in, ARTIFACTS);
        ArtifactFile artifactFile = new ArtifactFile();
        artifactFile.setFileUuid(uuid);
        artifactFile.setName(fileInfo.getKey());
        artifactFiles.add(artifactFile);
      }
      return artifactFiles;
    } catch (Exception ex) {
      return null;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
}
