package vast_research.spatial_core.resources;

import java.io.Serializable;

public class MonitorItem implements Serializable {

	public enum opType {
		DOWNLOAD, GET, INFO, NELEMENTS, SERVER, STATUS, UP, UPLOAD
	}

	private static final long serialVersionUID = 1L;

	private Long endTime;
	private final String operation;
	private final opType operationType;
	private final Long startTime;

	public MonitorItem(final opType operationType, final String operation,
			final Long startTime, final Long endTime) {
		super();
		this.endTime = endTime;
		this.operation = operation;
		this.operationType = operationType;
		this.startTime = startTime;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonitorItem other = (MonitorItem) obj;
		if (endTime == null) {
			if (other.endTime != null)
				return false;
		} else if (!endTime.equals(other.endTime))
			return false;
		if (operation == null) {
			if (other.operation != null)
				return false;
		} else if (!operation.equals(other.operation))
			return false;
		if (operationType == null) {
			if (other.operationType != null)
				return false;
		} else if (!operationType.equals(other.operationType))
			return false;
		if (startTime == null) {
			if (other.startTime != null)
				return false;
		} else if (!startTime.equals(other.startTime))
			return false;
		return true;
	}

	public Long getEndTime() {
		return endTime;
	}

	public String getOperation() {
		return operation;
	}

	public opType getOperationType() {
		return operationType;
	}

	public Long getStartTime() {
		return startTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (endTime == null ? 0 : endTime.hashCode());
		result = prime * result
				+ (operation == null ? 0 : operation.hashCode());
		result = prime * result
				+ (operationType == null ? 0 : operationType.hashCode());
		result = prime * result
				+ (startTime == null ? 0 : startTime.hashCode());
		return result;
	}

	public void setEndTime(final Long endTime) {
		this.endTime = endTime;
	}

	public String toJSON() {
		return "{\"type\":\"" + operationType + "\" , \"op\":\"" + operation
				+ "\", \"start\":" + startTime + " ,\"end\":" + endTime + "}";
	}

	@Override
	public String toString() {
		return "MonitorItem [operationType=" + operationType + ", operation="
				+ operation + ", startTime=" + startTime + ", endTime="
				+ endTime + "]";
	}

}
