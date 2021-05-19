/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.routines;

import io.harness.timescaledb.Public;

import org.jooq.Field;
import org.jooq.Parameter;
import org.jooq.impl.AbstractRoutine;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TimeBucket7 extends AbstractRoutine<Short> {
  private static final long serialVersionUID = 1L;

  /**
   * The parameter <code>public.time_bucket.RETURN_VALUE</code>.
   */
  public static final Parameter<Short> RETURN_VALUE =
      Internal.createParameter("RETURN_VALUE", SQLDataType.SMALLINT, false, false);

  /**
   * The parameter <code>public.time_bucket.bucket_width</code>.
   */
  public static final Parameter<Short> BUCKET_WIDTH =
      Internal.createParameter("bucket_width", SQLDataType.SMALLINT, false, false);

  /**
   * The parameter <code>public.time_bucket.ts</code>.
   */
  public static final Parameter<Short> TS = Internal.createParameter("ts", SQLDataType.SMALLINT, false, false);

  /**
   * Create a new routine call instance
   */
  public TimeBucket7() {
    super("time_bucket", Public.PUBLIC, SQLDataType.SMALLINT);

    setReturnParameter(RETURN_VALUE);
    addInParameter(BUCKET_WIDTH);
    addInParameter(TS);
    setOverloaded(true);
  }

  /**
   * Set the <code>bucket_width</code> parameter IN value to the routine
   */
  public void setBucketWidth(Short value) {
    setValue(BUCKET_WIDTH, value);
  }

  /**
   * Set the <code>bucket_width</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket7 setBucketWidth(Field<Short> field) {
    setField(BUCKET_WIDTH, field);
    return this;
  }

  /**
   * Set the <code>ts</code> parameter IN value to the routine
   */
  public void setTs(Short value) {
    setValue(TS, value);
  }

  /**
   * Set the <code>ts</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket7 setTs(Field<Short> field) {
    setField(TS, field);
    return this;
  }
}