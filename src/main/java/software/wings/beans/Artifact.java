package software.wings.beans;

import com.google.common.base.MoreObjects;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import software.wings.utils.validation.Create;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *  Artifact bean class.
 *
 *
 * @author Rishi
 *
 */
@Entity(value = "artifacts", noClassnameStored = true)
@Artifact.ValidArtifact
public class Artifact extends Base {
  public enum Status { NEW, RUNNING, QUEUED, WAITING, READY, ABORTED, FAILED, ERROR }

  @Indexed @Reference(idOnly = true) @NotNull private Application application;

  @Indexed @Reference(idOnly = true) @NotNull private Release release;

  @Indexed @NotNull(groups = Create.class) private String compName;

  @Indexed @NotNull(groups = Create.class) private String artifactSourceName;

  @Indexed @NotNull private String displayName;

  @Indexed @NotNull(groups = Create.class) private String revision;

  private ArtifactFile artifactFile;

  @Indexed private Status status;

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public String getCompName() {
    return compName;
  }

  public void setCompName(String compName) {
    this.compName = compName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  public ArtifactFile getArtifactFile() {
    return artifactFile;
  }

  public void setArtifactFile(ArtifactFile artifactFile) {
    this.artifactFile = artifactFile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    Artifact artifact = (Artifact) o;
    return Objects.equals(application, artifact.application) && Objects.equals(release, artifact.release)
        && Objects.equals(compName, artifact.compName)
        && Objects.equals(artifactSourceName, artifact.artifactSourceName)
        && Objects.equals(displayName, artifact.displayName) && Objects.equals(revision, artifact.revision)
        && Objects.equals(artifactFile, artifact.artifactFile) && status == artifact.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), application, release, compName, artifactSourceName, displayName, revision,
        artifactFile, status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("application", application)
        .add("release", release)
        .add("compName", compName)
        .add("artifactSourceName", artifactSourceName)
        .add("displayName", displayName)
        .add("revision", revision)
        .add("artifactFile", artifactFile)
        .add("status", status)
        .toString();
  }

  public static class Builder {
    private Application application;
    private Release release;
    private String compName;
    private String artifactSourceName;
    private String displayName;
    private String revision;
    private ArtifactFile artifactFile;
    private Status status;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anArtifact() {
      return new Builder();
    }

    public Builder withApplication(Application application) {
      this.application = application;
      return this;
    }

    public Builder withRelease(Release release) {
      this.release = release;
      return this;
    }

    public Builder withCompName(String compName) {
      this.compName = compName;
      return this;
    }

    public Builder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    public Builder withArtifactFile(ArtifactFile artifactFile) {
      this.artifactFile = artifactFile;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return anArtifact()
          .withApplication(application)
          .withRelease(release)
          .withCompName(compName)
          .withArtifactSourceName(artifactSourceName)
          .withDisplayName(displayName)
          .withRevision(revision)
          .withArtifactFile(artifactFile)
          .withStatus(status)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Artifact build() {
      Artifact artifact = new Artifact();
      artifact.setApplication(application);
      artifact.setRelease(release);
      artifact.setCompName(compName);
      artifact.setArtifactSourceName(artifactSourceName);
      artifact.setDisplayName(displayName);
      artifact.setRevision(revision);
      artifact.setArtifactFile(artifactFile);
      artifact.setStatus(status);
      artifact.setUuid(uuid);
      artifact.setCreatedBy(createdBy);
      artifact.setCreatedAt(createdAt);
      artifact.setLastUpdatedBy(lastUpdatedBy);
      artifact.setLastUpdatedAt(lastUpdatedAt);
      artifact.setActive(active);
      return artifact;
    }
  }

  /**
   * Created by peeyushaggarwal on 4/4/16.
   */

  @Retention(RetentionPolicy.RUNTIME)
  @Constraint(validatedBy = ValidArtifact.Validator.class)
  public static @interface ValidArtifact {
    String
    message() default "bean isNotBlank(bean.getApplication().getUuid()) have id for updating and application id is not same.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public class Validator implements ConstraintValidator<ValidArtifact, Artifact> {
      @Override
      public void initialize(final ValidArtifact validateForUpdate) {}

      @Override
      public boolean isValid(final Artifact bean, final ConstraintValidatorContext constraintValidatorContext) {
        return bean.getApplication() != null && isNotBlank(bean.getApplication().getUuid())
            && isNotBlank(bean.getRelease().getUuid());
      }
    }
  }
}
