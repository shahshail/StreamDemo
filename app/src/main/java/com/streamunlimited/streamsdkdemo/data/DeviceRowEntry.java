package com.streamunlimited.streamsdkdemo.data;

public class DeviceRowEntry implements Comparable<DeviceRowEntry> {

    protected String _ipAddress;
    protected int _port;
    protected String _name;
    private String _uuid;
    private String _sourceUUID;
    private boolean _multiroomSupported;

    public DeviceRowEntry(String ipAddress, int port, String name, String uuid) {
        this._ipAddress = ipAddress;
        this._port = port;
        this._name = name;
        this._uuid = uuid;
    }

    public String getUUID() {
        return _uuid;
    }

    public void setUUID(String uuid) {
        this._uuid = uuid;
    }

    public String getSourceUUID() {
        return this._sourceUUID;
    }

    public void setSourceUUID(String sourceUUID) {
        this._sourceUUID = sourceUUID;
    }

    public String getIpAddress() {
        return _ipAddress;
    }

    public void setIpAddress(String _ipAddress) {
        this._ipAddress = _ipAddress;
    }

    public int getPort() { return _port; }

    public void setPort(int _port) { this._port = _port; }

    public String getName() {
        return _name;
    }

    public void setName(String _name) {
        this._name = _name;
    }

    public boolean getMultiroomSupported() {
        return this._multiroomSupported;
    }

    public void setMultiroomSupported(boolean _multiroomSuppoerted) {
        this._multiroomSupported = _multiroomSuppoerted;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_uuid == null) ? 0 : _uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DeviceRowEntry other = (DeviceRowEntry) obj;
        if (_uuid == null) {
            if (other._uuid != null) {
                return false;
            }
        } else if (!_uuid.equals(other._uuid)) {
            return false;
        }
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (this.getMultiroomSupported() != other.getMultiroomSupported()) {
            return false;
        }
        if (!this.getSourceUUID().equals(other.getSourceUUID())) {
            return false;
        }
        if (!this.getIpAddress().equals(other.getIpAddress())) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(DeviceRowEntry another) {
        if (!this.getName().equals(another.getName())) {
            return this.getName().compareTo(another.getName());
        } else {
            return this.getUUID().compareTo(another.getUUID());
        }
    }

}
