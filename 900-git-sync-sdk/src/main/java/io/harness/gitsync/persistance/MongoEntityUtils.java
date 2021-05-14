package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.persistence.PersistentEntity;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.data.annotation.Id;

@UtilityClass
@OwnedBy(DX)
public class MongoEntityUtils {
  // return true if entity is new.
  public boolean isNewEntity(PersistentEntity object) {
    final Field[] declaredFields = object.getClass().getDeclaredFields();
    final Optional<Field> idField =
        Arrays.stream(declaredFields).filter(field -> field.getAnnotation(Id.class) != null).findFirst();
    return idField
        .map(field -> {
          try {
            field.setAccessible(true);
            final Object o = field.get(object);
            return o == null || o.toString().isEmpty();
          } catch (IllegalAccessException e) {
            // this exception is never reachable.
            throw new GeneralException("Can't get id field in entity.", e);
          }
        })
        .orElseThrow(() -> new GeneralException("Id field not present in entity"));
  }
}