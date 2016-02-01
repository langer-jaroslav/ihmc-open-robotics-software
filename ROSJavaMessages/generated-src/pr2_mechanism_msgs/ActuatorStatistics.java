package pr2_mechanism_msgs;

public interface ActuatorStatistics extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "pr2_mechanism_msgs/ActuatorStatistics";
  static final java.lang.String _DEFINITION = "# This message contains the state of an actuator on the pr2 robot.\n# An actuator contains a motor and an encoder, and is connected\n# to a joint by a transmission\n\n# the name of the actuator\nstring name\n\n# the sequence number of the MCB in the ethercat chain. \n# the first device in the chain gets deviced_id zero\nint32 device_id\n\n# the time at which this actuator state was measured\ntime timestamp\n\n# the encoder position, represented by the number of encoder ticks\nint32 encoder_count\n\n# The angular offset (in radians) that is added to the encoder reading, \n# to get to the position of the actuator. This number is computed when the referece\n# sensor is triggered during the calibration phase\nfloat64 encoder_offset\n\n# the encoder position in radians\nfloat64 position\n\n# the encoder velocity in encoder ticks per second\nfloat64 encoder_velocity\n\n# the encoder velocity in radians per second\nfloat64 velocity\n\n# the value of the calibration reading: low (false) or high (true)\nbool calibration_reading\n\n# bool to indicate if the joint already triggered the rising/falling edge of the reference sensor\nbool calibration_rising_edge_valid\nbool calibration_falling_edge_valid\n\n# the encoder position when the last rising/falling edge was observed. \n# only read this value when the calibration_rising/falling_edge_valid is true\nfloat64 last_calibration_rising_edge\nfloat64 last_calibration_falling_edge\n\n# flag to indicate if this actuator is enabled or not. \n# An actuator can only be commanded when it is enabled.\nbool is_enabled\n\n# indicates if the motor is halted. A motor can be halted because of a voltage or communication problem\nbool halted\n\n# the last current/effort command that was requested\nfloat64 last_commanded_current\nfloat64 last_commanded_effort\n\n# the last current/effort command that was executed by the actuator\nfloat64 last_executed_current\nfloat64 last_executed_effort\n\n# the last current/effort that was measured by the actuator\nfloat64 last_measured_current\nfloat64 last_measured_effort\n\n# the motor voltate\nfloat64 motor_voltage\n\n# the number of detected encoder problems \nint32 num_encoder_errors\n\n";
  java.lang.String getName();
  void setName(java.lang.String value);
  int getDeviceId();
  void setDeviceId(int value);
  org.ros.message.Time getTimestamp();
  void setTimestamp(org.ros.message.Time value);
  int getEncoderCount();
  void setEncoderCount(int value);
  double getEncoderOffset();
  void setEncoderOffset(double value);
  double getPosition();
  void setPosition(double value);
  double getEncoderVelocity();
  void setEncoderVelocity(double value);
  double getVelocity();
  void setVelocity(double value);
  boolean getCalibrationReading();
  void setCalibrationReading(boolean value);
  boolean getCalibrationRisingEdgeValid();
  void setCalibrationRisingEdgeValid(boolean value);
  boolean getCalibrationFallingEdgeValid();
  void setCalibrationFallingEdgeValid(boolean value);
  double getLastCalibrationRisingEdge();
  void setLastCalibrationRisingEdge(double value);
  double getLastCalibrationFallingEdge();
  void setLastCalibrationFallingEdge(double value);
  boolean getIsEnabled();
  void setIsEnabled(boolean value);
  boolean getHalted();
  void setHalted(boolean value);
  double getLastCommandedCurrent();
  void setLastCommandedCurrent(double value);
  double getLastCommandedEffort();
  void setLastCommandedEffort(double value);
  double getLastExecutedCurrent();
  void setLastExecutedCurrent(double value);
  double getLastExecutedEffort();
  void setLastExecutedEffort(double value);
  double getLastMeasuredCurrent();
  void setLastMeasuredCurrent(double value);
  double getLastMeasuredEffort();
  void setLastMeasuredEffort(double value);
  double getMotorVoltage();
  void setMotorVoltage(double value);
  int getNumEncoderErrors();
  void setNumEncoderErrors(int value);
}
