/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package edu.berkeley.gamesman.avro;

@SuppressWarnings("all")
public class PositionValue extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"PositionValue\",\"namespace\":\"edu.berkeley.gamesman.avro\",\"fields\":[{\"name\":\"position\",\"type\":\"string\"},{\"name\":\"value\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"Value\",\"symbols\":[\"WIN\",\"LOSE\",\"TIE\",\"DRAW\"]}]},{\"name\":\"remoteness\",\"type\":[\"null\",\"int\"]},{\"name\":\"winBy\",\"type\":[\"null\",\"int\"]},{\"name\":\"mex\",\"type\":[\"null\",\"int\"]}]}");
  public java.lang.CharSequence position;
  public edu.berkeley.gamesman.avro.Value value;
  public java.lang.Integer remoteness;
  public java.lang.Integer winBy;
  public java.lang.Integer mex;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return position;
    case 1: return value;
    case 2: return remoteness;
    case 3: return winBy;
    case 4: return mex;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: position = (java.lang.CharSequence)value$; break;
    case 1: value = (edu.berkeley.gamesman.avro.Value)value$; break;
    case 2: remoteness = (java.lang.Integer)value$; break;
    case 3: winBy = (java.lang.Integer)value$; break;
    case 4: mex = (java.lang.Integer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}