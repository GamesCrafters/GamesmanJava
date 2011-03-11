/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package edu.berkeley.gamesman.avro;

@SuppressWarnings("all")
public class Fields extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"Fields\",\"namespace\":\"edu.berkeley.gamesman.avro\",\"fields\":[{\"name\":\"value\",\"type\":\"boolean\",\"default\":false},{\"name\":\"remoteness\",\"type\":\"boolean\",\"default\":false},{\"name\":\"winBy\",\"type\":\"boolean\",\"default\":false},{\"name\":\"mex\",\"type\":\"boolean\",\"default\":false}]}");
  public boolean value;
  public boolean remoteness;
  public boolean winBy;
  public boolean mex;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return value;
    case 1: return remoteness;
    case 2: return winBy;
    case 3: return mex;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: value = (java.lang.Boolean)value$; break;
    case 1: remoteness = (java.lang.Boolean)value$; break;
    case 2: winBy = (java.lang.Boolean)value$; break;
    case 3: mex = (java.lang.Boolean)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}
