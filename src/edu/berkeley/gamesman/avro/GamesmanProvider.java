/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package edu.berkeley.gamesman.avro;

@SuppressWarnings("all")
public interface GamesmanProvider {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"GamesmanProvider\",\"namespace\":\"edu.berkeley.gamesman.avro\",\"types\":[{\"type\":\"enum\",\"name\":\"Value\",\"symbols\":[\"WIN\",\"LOSE\",\"TIE\",\"DRAW\"]},{\"type\":\"record\",\"name\":\"Fields\",\"fields\":[{\"name\":\"value\",\"type\":\"boolean\",\"default\":false},{\"name\":\"remoteness\",\"type\":\"boolean\",\"default\":false},{\"name\":\"winBy\",\"type\":\"boolean\",\"default\":false},{\"name\":\"mex\",\"type\":\"boolean\",\"default\":false}]},{\"type\":\"record\",\"name\":\"PositionValue\",\"fields\":[{\"name\":\"position\",\"type\":\"string\"},{\"name\":\"value\",\"type\":[\"null\",\"Value\"]},{\"name\":\"remoteness\",\"type\":[\"null\",\"int\"]},{\"name\":\"winBy\",\"type\":[\"null\",\"int\"]},{\"name\":\"mex\",\"type\":[\"null\",\"int\"]}]},{\"type\":\"record\",\"name\":\"VariantSupport\",\"fields\":[{\"name\":\"variant\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"fields\",\"type\":\"Fields\"}]}],\"messages\":{\"getSupportedGames\":{\"request\":[],\"response\":{\"type\":\"map\",\"values\":{\"type\":\"array\",\"items\":\"VariantSupport\"}}},\"getInitialPositionValue\":{\"request\":[{\"name\":\"game\",\"type\":\"string\"},{\"name\":\"variant\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"fields\",\"type\":\"Fields\"}],\"response\":\"PositionValue\"},\"getPositionValues\":{\"request\":[{\"name\":\"game\",\"type\":\"string\"},{\"name\":\"variant\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"positions\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"fields\",\"type\":\"Fields\"}],\"response\":{\"type\":\"map\",\"values\":\"PositionValue\"}},\"getNextPositionValues\":{\"request\":[{\"name\":\"game\",\"type\":\"string\"},{\"name\":\"variant\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"position\",\"type\":\"string\"},{\"name\":\"fields\",\"type\":\"Fields\"}],\"response\":{\"type\":\"map\",\"values\":\"PositionValue\"}},\"solve\":{\"request\":[{\"name\":\"game\",\"type\":\"string\"},{\"name\":\"variant\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"pingback\",\"type\":\"string\"}],\"response\":\"null\",\"one-way\":true}}}");
  java.util.Map<java.lang.CharSequence,java.util.List<edu.berkeley.gamesman.avro.VariantSupport>> getSupportedGames() throws org.apache.avro.ipc.AvroRemoteException;
  edu.berkeley.gamesman.avro.PositionValue getInitialPositionValue(java.lang.CharSequence game, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> variant, edu.berkeley.gamesman.avro.Fields fields) throws org.apache.avro.ipc.AvroRemoteException;
  java.util.Map<java.lang.CharSequence,edu.berkeley.gamesman.avro.PositionValue> getPositionValues(java.lang.CharSequence game, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> variant, java.util.List<java.lang.CharSequence> positions, edu.berkeley.gamesman.avro.Fields fields) throws org.apache.avro.ipc.AvroRemoteException;
  java.util.Map<java.lang.CharSequence,edu.berkeley.gamesman.avro.PositionValue> getNextPositionValues(java.lang.CharSequence game, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> variant, java.lang.CharSequence position, edu.berkeley.gamesman.avro.Fields fields) throws org.apache.avro.ipc.AvroRemoteException;
  void solve(java.lang.CharSequence game, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> variant, java.lang.CharSequence pingback);
}