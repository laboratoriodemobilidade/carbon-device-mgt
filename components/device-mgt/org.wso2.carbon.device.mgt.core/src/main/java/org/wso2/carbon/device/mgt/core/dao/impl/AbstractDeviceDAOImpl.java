/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.core.dao.impl;

import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.EnrolmentInfo;
import org.wso2.carbon.device.mgt.common.EnrolmentInfo.Status;
import org.wso2.carbon.device.mgt.common.PaginationRequest;
import org.wso2.carbon.device.mgt.core.dao.DeviceDAO;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOException;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOFactory;
import org.wso2.carbon.device.mgt.core.dao.util.DeviceManagementDAOUtil;
import org.wso2.carbon.device.mgt.core.dto.DeviceType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractDeviceDAOImpl implements DeviceDAO {

    @Override
    public int addDevice(int typeId, Device device, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int deviceId = -1;
        try {
            conn = this.getConnection();
            String sql = "INSERT INTO DM_DEVICE(DESCRIPTION, NAME, DEVICE_TYPE_ID, DEVICE_IDENTIFICATION, TENANT_ID) " +
                    "VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, new String[] {"id"});
            stmt.setString(1, device.getDescription());
            stmt.setString(2, device.getName());
            stmt.setInt(3, typeId);
            stmt.setString(4, device.getDeviceIdentifier());
            stmt.setInt(5, tenantId);
            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                deviceId = rs.getInt(1);
            }
            return deviceId;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while enrolling device '" + device.getName() +
                    "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    @Override
    public boolean updateDevice(int typeId, Device device, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        boolean status = false;
        int rows;
        try {
            conn = this.getConnection();
            String sql = "UPDATE DM_DEVICE SET DESCRIPTION = ?, NAME = ? WHERE DEVICE_IDENTIFICATION = ? AND " +
                    "DEVICE_TYPE_ID = ? AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql, new String[] {"id"});
            stmt.setString(1, device.getDescription());
            stmt.setString(2, device.getName());
            stmt.setString(3, device.getDeviceIdentifier());
            stmt.setInt(4, typeId);
            stmt.setInt(5, tenantId);
            rows = stmt.executeUpdate();
            if (rows > 0) {
                status = true;
            }
            return status;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while enrolling device '" +
                    device.getName() + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
    }

    @Override
    public int removeDevice(DeviceIdentifier deviceId, int tenantId) throws DeviceManagementDAOException {
        return 0;
    }

    @Override
    public Device getDevice(DeviceIdentifier deviceIdentifier, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Device device = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT d1.ID AS DEVICE_ID, d1.DESCRIPTION, d1.NAME AS DEVICE_NAME, d1.DEVICE_TYPE, " +
                    "d1.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                    "e.DATE_OF_ENROLMENT, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e, (SELECT d.ID, d.DESCRIPTION, d.NAME, " +
                    "t.NAME AS DEVICE_TYPE, d.DEVICE_IDENTIFICATION FROM DM_DEVICE d, DM_DEVICE_TYPE t WHERE " +
                    "t.NAME = ? AND d.DEVICE_IDENTIFICATION = ? AND d.TENANT_ID = ?) d1 WHERE d1.ID = e.DEVICE_ID " +
                    "AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceIdentifier.getType());
            stmt.setString(2, deviceIdentifier.getId());
            stmt.setInt(3, tenantId);
            stmt.setInt(4, tenantId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                device = DeviceManagementDAOUtil.loadMatchingDevice(rs);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while listing devices for type " +
                    "'" + deviceIdentifier.getType() + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return device;
    }

    @Override
    public Device getDevice(DeviceIdentifier deviceIdentifier, EnrolmentInfo.Status status, int tenantId) throws
                                                                                         DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Device device = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT d1.ID AS DEVICE_ID, d1.DESCRIPTION, d1.NAME AS DEVICE_NAME, d1.DEVICE_TYPE, " +
                         "d1.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                         "e.DATE_OF_ENROLMENT, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e, (SELECT d.ID, d.DESCRIPTION, d.NAME, " +
                         "t.NAME AS DEVICE_TYPE, d.DEVICE_IDENTIFICATION FROM DM_DEVICE d, DM_DEVICE_TYPE t WHERE " +
                         "t.NAME = ? AND d.DEVICE_IDENTIFICATION = ? AND d.TENANT_ID = ?) d1 WHERE d1.ID = e.DEVICE_ID " +
                         "AND TENANT_ID = ? AND e.STATUS = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceIdentifier.getType());
            stmt.setString(2, deviceIdentifier.getId());
            stmt.setInt(3, tenantId);
            stmt.setInt(4, tenantId);
            stmt.setString(5, status.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                device = DeviceManagementDAOUtil.loadDevice(rs);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while listing devices for type " +
                                                   "'" + deviceIdentifier.getType() + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return device;
    }

    @Override
    public HashMap<Integer, Device> getDevice(DeviceIdentifier deviceIdentifier) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Device device;
        HashMap<Integer, Device> deviceHashMap = new HashMap<>();
        try {
            conn = this.getConnection();
            String sql = "SELECT d1.ID AS DEVICE_ID, d1.DESCRIPTION, d1.NAME AS DEVICE_NAME, d1.DEVICE_TYPE, e.TENANT_ID, " +
                    "d1.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                    "e.DATE_OF_ENROLMENT, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e, (SELECT d.ID, d.DESCRIPTION, d.NAME, " +
                    "t.NAME AS DEVICE_TYPE, d.DEVICE_IDENTIFICATION FROM DM_DEVICE d, DM_DEVICE_TYPE t WHERE " +
                    "t.NAME = ? AND d.DEVICE_IDENTIFICATION = ? ) d1 WHERE d1.ID = e.DEVICE_ID ";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceIdentifier.getType());
            stmt.setString(2, deviceIdentifier.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {
                device = DeviceManagementDAOUtil.loadDevice(rs);
                deviceHashMap.put(rs.getInt("TENANT_ID"), device);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while listing devices for type " +
                    "'" + deviceIdentifier.getType() + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return deviceHashMap;
    }

    @Override
    public Device getDevice(int deviceId, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Device device = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT d1.ID AS DEVICE_ID, d1.DESCRIPTION, d1.NAME AS DEVICE_NAME, d1.DEVICE_TYPE, " +
                         "d1.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                         "e.DATE_OF_ENROLMENT, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e, (SELECT d.ID, d.DESCRIPTION, d.NAME, " +
                         "t.NAME AS DEVICE_TYPE, d.DEVICE_IDENTIFICATION FROM DM_DEVICE d, DM_DEVICE_TYPE t WHERE " +
                         "d.ID = ? AND d.TENANT_ID = ?) d1 WHERE d1.ID = e.DEVICE_ID " +
                         "AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, deviceId);
            stmt.setInt(2, tenantId);
            stmt.setInt(3, tenantId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                device = DeviceManagementDAOUtil.loadDevice(rs);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while retrieving device for id " +
                                                   "'" + deviceId + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return device;
    }

    @Override
    public List<Device> getDevices(int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Device> devices = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT d1.DEVICE_ID, d1.DESCRIPTION, d1.NAME AS DEVICE_NAME, d1.DEVICE_TYPE, " +
                    "d1.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                    "e.DATE_OF_ENROLMENT, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e, (SELECT d.ID AS DEVICE_ID, " +
                    "d.DESCRIPTION, d.NAME, d.DEVICE_IDENTIFICATION, t.NAME AS DEVICE_TYPE FROM DM_DEVICE d, " +
                    "DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID AND d.TENANT_ID = ?) d1 " +
                    "WHERE d1.DEVICE_ID = e.DEVICE_ID AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setInt(2, tenantId);
            rs = stmt.executeQuery();
            devices = new ArrayList<>();
            while (rs.next()) {
                Device device = DeviceManagementDAOUtil.loadDevice(rs);
                devices.add(device);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while retrieving information of all " +
                    "registered devices", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return devices;
    }

    @Override
    public List<Device> getDevices(String type, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Device> devices = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT d1.ID AS DEVICE_ID, d1.DESCRIPTION, d1.NAME AS DEVICE_NAME, d1.DEVICE_TYPE, " +
                    "d1.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                    "e.DATE_OF_ENROLMENT, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e, (SELECT d.ID, d.DESCRIPTION, " +
                    "d.NAME, d.DEVICE_IDENTIFICATION, t.NAME AS DEVICE_TYPE FROM DM_DEVICE d, " +
                    "DM_DEVICE_TYPE t WHERE DEVICE_TYPE_ID = t.ID AND t.NAME = ? " +
                    "AND d.TENANT_ID = ?) d1 WHERE d1.ID = e.DEVICE_ID AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, type);
            stmt.setInt(2, tenantId);
            stmt.setInt(3, tenantId);
            rs = stmt.executeQuery();
            devices = new ArrayList<>();
            while (rs.next()) {
                Device device = DeviceManagementDAOUtil.loadDevice(rs);
                devices.add(device);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while listing devices for type '" + type + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return devices;
    }

    @Override
    public List<Device> getDevicesOfUser(String username, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Device> devices = new ArrayList<>();
        try {
            conn = this.getConnection();
            String sql = "SELECT e1.OWNER, e1.OWNERSHIP, e1.ENROLMENT_ID, e1.DEVICE_ID, e1.STATUS, e1.DATE_OF_LAST_UPDATE," +
                         " e1.DATE_OF_ENROLMENT, d.DESCRIPTION, d.NAME AS DEVICE_NAME, d.DEVICE_IDENTIFICATION, t.NAME " +
                         "AS DEVICE_TYPE FROM DM_DEVICE d, (SELECT e.OWNER, e.OWNERSHIP, e.ID AS ENROLMENT_ID, " +
                         "e.DEVICE_ID, e.STATUS, e.DATE_OF_LAST_UPDATE, e.DATE_OF_ENROLMENT FROM DM_ENROLMENT e WHERE " +
                         "e.TENANT_ID = ? AND e.OWNER = ?) e1, DM_DEVICE_TYPE t WHERE d.ID = e1.DEVICE_ID " +
                         "AND t.ID = d.DEVICE_TYPE_ID";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setString(2, username);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Device device = DeviceManagementDAOUtil.loadDevice(rs);
                devices.add(device);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the list of devices belongs to '" +
                    username + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return devices;
    }

    private Connection getConnection() throws SQLException {
        return DeviceManagementDAOFactory.getConnection();
    }

    /**
     * Get device count of all devices.
     *
     * @return device count
     * @throws DeviceManagementDAOException
     */
    @Override
    public int getDeviceCount(int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int deviceCount = 0;
        try {
            conn = this.getConnection();
            String sql = "SELECT COUNT(d1.DEVICE_ID) AS DEVICE_COUNT FROM DM_ENROLMENT e, (SELECT d.ID AS DEVICE_ID FROM " +
                  "DM_DEVICE d, DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID AND d.TENANT_ID = ?) d1 WHERE " +
                  "d1.DEVICE_ID = e.DEVICE_ID AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setInt(2, tenantId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                deviceCount = rs.getInt("DEVICE_COUNT");
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while getting the device count", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return deviceCount;
    }

    @Override
    public int getDeviceCount(PaginationRequest request, int tenantId) throws DeviceManagementDAOException {
        int deviceCount = 0;
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String deviceType = request.getDeviceType();
        boolean isDeviceTypeProvided = false;
        String deviceName = request.getDeviceName();
        boolean isDeviceNameProvided = false;
        String owner = request.getOwner();
        boolean isOwnerProvided = false;
        String ownership = request.getOwnership();
        boolean isOwnershipProvided = false;
        String status = request.getStatus();
        boolean isStatusProvided = false;
        try {
            conn = this.getConnection();
            String sql = "SELECT COUNT(d1.ID) AS DEVICE_COUNT FROM DM_ENROLMENT e, (SELECT d.ID, d.NAME, d.DEVICE_IDENTIFICATION, " +
                         "t.NAME AS DEVICE_TYPE FROM DM_DEVICE d, DM_DEVICE_TYPE t WHERE DEVICE_TYPE_ID = t.ID " +
                         "AND d.TENANT_ID = ?";

            if (deviceType != null && !deviceType.isEmpty()) {
                sql = sql + " AND t.NAME = ?";
                isDeviceTypeProvided = true;
            }

            if (deviceName != null && !deviceName.isEmpty()) {
                sql = sql + " AND d.NAME LIKE ?";
                isDeviceNameProvided = true;
            }

            sql = sql + ") d1 WHERE d1.ID = e.DEVICE_ID AND TENANT_ID = ?";

            if (ownership != null && !ownership.isEmpty()) {
                sql = sql + " AND e.OWNERSHIP = ?";
                isOwnershipProvided = true;
            }

            if (owner != null && !owner.isEmpty()) {
                sql = sql + " AND e.OWNER LIKE ?";
                isOwnerProvided = true;
            }

            if (status != null && !status.isEmpty()) {
                sql = sql + " AND e.STATUS = ?";
                isStatusProvided = true;
            }

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            int paramIdx = 2;
            if (isDeviceTypeProvided) {
                stmt.setString(paramIdx++, request.getDeviceType());
            }
            if (isDeviceNameProvided) {
                stmt.setString(paramIdx++, request.getDeviceName() + "%");
            }
            stmt.setInt(paramIdx++, tenantId);
            if (isOwnershipProvided) {
                stmt.setString(paramIdx++, request.getOwnership());
            }
            if (isOwnerProvided) {
                stmt.setString(paramIdx++, request.getOwner() + "%");
            }
            if (isStatusProvided) {
                stmt.setString(paramIdx++, request.getStatus());
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                deviceCount = rs.getInt("DEVICE_COUNT");
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while retrieving information of all " +
                                                   "registered devices", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return deviceCount;
    }

    @Override
    public int getDeviceCountByType(String type, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int deviceCount = 0;
        try {
            conn = this.getConnection();
            String sql = "SELECT COUNT(d1.ID) AS DEVICE_COUNT FROM DM_ENROLMENT e, (SELECT d.ID FROM DM_DEVICE d, " +
            "DM_DEVICE_TYPE t WHERE DEVICE_TYPE_ID = t.ID AND t.NAME = ? " +
            "AND d.TENANT_ID = ?) d1 WHERE d1.ID = e.DEVICE_ID AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, type);
            stmt.setInt(2, tenantId);
            stmt.setInt(3, tenantId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                deviceCount = rs.getInt("DEVICE_COUNT");
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while getting the device count", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return deviceCount;
    }

    @Override
    public int getDeviceCountByUser(String username, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        int deviceCount = 0;
        try {
            conn = this.getConnection();
            String sql = "SELECT COUNT(e1.DEVICE_ID) AS DEVICE_COUNT FROM DM_DEVICE d, (SELECT e.DEVICE_ID " +
                         "FROM DM_ENROLMENT e WHERE e.TENANT_ID = ? AND e.OWNER = ?) " +
                         "e1, DM_DEVICE_TYPE t WHERE d.ID = e1.DEVICE_ID AND t.ID = d.DEVICE_TYPE_ID";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                deviceCount = rs.getInt("DEVICE_COUNT");
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the list of devices belongs to '" +
                                                   username + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
        return deviceCount;
    }

    @Override
    public int getDeviceCountByName(String deviceName, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        int deviceCount = 0;
        try {
            conn = this.getConnection();
            String sql = "SELECT COUNT(d1.ID) AS DEVICE_COUNT FROM DM_ENROLMENT e, (SELECT d.ID FROM DM_DEVICE d, " +
                         "DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID AND d.NAME LIKE ? AND d.TENANT_ID = ?) d1 " +
                         "WHERE d1.ID = e.DEVICE_ID AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceName + "%");
            stmt.setInt(2, tenantId);
            stmt.setInt(3, tenantId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                deviceCount = rs.getInt("DEVICE_COUNT");
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the device count that matches " +
                                                   "'" + deviceName + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
        return deviceCount;
    }

    @Override
    public int getDeviceCountByOwnership(String ownerShip, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        int deviceCount = 0;
        try {
            conn = this.getConnection();
            String sql = "SELECT COUNT(d.ID) AS DEVICE_COUNT FROM (SELECT e.DEVICE_ID FROM DM_ENROLMENT e WHERE " +
                         "TENANT_ID = ? AND OWNERSHIP = ?) e, DM_DEVICE d, " +
                         "DM_DEVICE_TYPE t WHERE d.ID = e.DEVICE_ID AND d.DEVICE_TYPE_ID = t.ID AND d.TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setString(2, ownerShip);
            stmt.setInt(3, tenantId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                deviceCount = rs.getInt("DEVICE_COUNT");
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the list of devices that matches to ownership " +
                                                   "'" + ownerShip + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
        return deviceCount;
    }

    @Override
    public int getDeviceCountByStatus(String status, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        int deviceCount = 0;
        try {
            conn = this.getConnection();
            String sql = "SELECT COUNT(d.ID) AS DEVICE_COUNT FROM (SELECT e.DEVICE_ID FROM DM_ENROLMENT e WHERE " +
                         "TENANT_ID = ? AND STATUS = ?) e, DM_DEVICE d, " +
                         "DM_DEVICE_TYPE t WHERE d.ID = e.DEVICE_ID AND d.DEVICE_TYPE_ID = t.ID AND d.TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setString(2, status);
            stmt.setInt(3, tenantId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                deviceCount = rs.getInt("DEVICE_COUNT");
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the list of devices that matches to status " +
                                                   "'" + status + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
        return deviceCount;
    }

    /**
     * Get the list of devices that matches with the given device name.
     *
     * @param deviceName Name of the device.
     * @param tenantId   Id of the current tenant
     * @return device list
     * @throws DeviceManagementDAOException
     */
    @Override
    public List<Device> getDevicesByName(String deviceName, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        List<Device> devices = new ArrayList<>();
        ResultSet rs = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT d1.ID AS DEVICE_ID, d1.DESCRIPTION, d1.NAME AS DEVICE_NAME, d1.DEVICE_TYPE, " +
                    "d1.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                    "e.DATE_OF_ENROLMENT, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e, (SELECT d.ID, d.NAME, " +
                    "d.DESCRIPTION, t.NAME AS DEVICE_TYPE, d.DEVICE_IDENTIFICATION FROM DM_DEVICE d, " +
                    "DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID AND d.NAME LIKE ? AND d.TENANT_ID = ?) d1 " +
                    "WHERE DEVICE_ID = e.DEVICE_ID AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceName + "%");
            stmt.setInt(2, tenantId);
            stmt.setInt(3, tenantId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Device device = DeviceManagementDAOUtil.loadDevice(rs);
                devices.add(device);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the list of devices that matches " +
                    "'" + deviceName + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return devices;
    }

    @Override
    public int addEnrollment(Device device, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int enrolmentId = -1;
        try {
            conn = this.getConnection();
            String sql = "INSERT INTO DM_ENROLMENT(DEVICE_ID, OWNER, OWNERSHIP, STATUS,DATE_OF_ENROLMENT, " +
                    "DATE_OF_LAST_UPDATE, TENANT_ID) VALUES(?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, new String[] {"id"});
            stmt.setInt(1, device.getId());
            stmt.setString(2, device.getEnrolmentInfo().getOwner());
            stmt.setString(3, device.getEnrolmentInfo().getOwnership().toString());
            stmt.setString(4, device.getEnrolmentInfo().getStatus().toString());
            stmt.setTimestamp(5, new Timestamp(new Date().getTime()));
            stmt.setTimestamp(6, new Timestamp(new Date().getTime()));
            stmt.setInt(7, tenantId);
            stmt.execute();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                enrolmentId = rs.getInt(1);
            }
            return enrolmentId;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while adding enrolment", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    @Override
    public boolean setEnrolmentStatus(DeviceIdentifier deviceId, String currentOwner, Status status,
                                      int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        try {
            conn = this.getConnection();
            String sql = "UPDATE DM_ENROLMENT SET STATUS = ? WHERE DEVICE_ID = (SELECT d.ID FROM DM_DEVICE d, " +
                    "DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID AND d.DEVICE_IDENTIFICATION = ? " +
                    "AND t.NAME = ? AND d.TENANT_ID = ?) AND OWNER = ? AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status.toString());
            stmt.setString(2, deviceId.getId());
            stmt.setString(3, deviceId.getType());
            stmt.setInt(4, tenantId);
            stmt.setString(5, currentOwner);
            stmt.setInt(6, tenantId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while setting the status of device enrolment", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
        return true;
    }

    @Override
    public Status getEnrolmentStatus(DeviceIdentifier deviceId, String currentOwner,
                                     int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Status status = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT STATUS FROM DM_ENROLMENT WHERE DEVICE_ID = (SELECT d.ID FROM DM_DEVICE d, " +
                    "DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID AND d.DEVICE_IDENTIFICATION = ? " +
                    "AND t.NAME = ? AND d.TENANT_ID = ?) AND OWNER = ? AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceId.getId());
            stmt.setString(2, deviceId.getType());
            stmt.setInt(3, tenantId);
            stmt.setString(4, currentOwner);
            stmt.setInt(5, tenantId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                status = Status.valueOf(rs.getString("STATUS"));
            }
            return status;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while retrieving the status of device enrolment", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    @Override
    public EnrolmentInfo getEnrolment(DeviceIdentifier deviceId, String currentOwner,
                                      int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        EnrolmentInfo enrolmentInfo = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT ID AS ENROLMENT_ID, DEVICE_ID, OWNER, OWNERSHIP, STATUS, DATE_OF_ENROLMENT, " +
                    "DATE_OF_LAST_UPDATE, TENANT_ID FROM DM_ENROLMENT WHERE DEVICE_ID = (SELECT d.ID " +
                    "FROM DM_DEVICE d, DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID " +
                    "AND d.DEVICE_IDENTIFICATION = ? AND t.NAME = ? AND d.TENANT_ID = ?) " +
                    "AND OWNER = ? AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceId.getId());
            stmt.setString(2, deviceId.getType());
            stmt.setInt(3, tenantId);
            stmt.setString(4, currentOwner);
            stmt.setInt(5, tenantId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                enrolmentInfo = DeviceManagementDAOUtil.loadEnrolment(rs);
            }
            return enrolmentInfo;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while retrieving the enrolment " +
                    "information of user '" + currentOwner + "' upon device '" + deviceId + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    public int getEnrolmentByStatus(DeviceIdentifier deviceId, Status status,
                                    int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = this.getConnection();
            String sql = "SELECT ID AS ENROLMENT_ID FROM DM_ENROLMENT WHERE DEVICE_ID = (SELECT d.ID " +
                    "FROM DM_DEVICE d, DM_DEVICE_TYPE t WHERE d.DEVICE_TYPE_ID = t.ID " +
                    "AND d.DEVICE_IDENTIFICATION = ? AND t.NAME = ? AND d.TENANT_ID = ?) " +
                    "AND STATUS = ? AND TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceId.getId());
            stmt.setString(2, deviceId.getType());
            stmt.setInt(3, tenantId);
            stmt.setString(4, status.toString());
            stmt.setInt(5, tenantId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ENROLMENT_ID");
            } else {
                return -1; // if no results found
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while retrieving the enrolment " +
                    "id of device '" + deviceId + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    public List<EnrolmentInfo> getEnrolmentsByStatus(List<DeviceIdentifier> deviceIds, Status status,
                                                     int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<EnrolmentInfo> enrolments = new ArrayList<>();
        try {
            conn = this.getConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT e.ID AS ENROLMENT_ID, e.OWNER, e.OWNERSHIP, e.DATE_OF_ENROLMENT, e.DATE_OF_LAST_UPDATE, " +
                    "e.STATUS FROM DM_ENROLMENT e WHERE e.DEVICE_ID IN (SELECT d.ID FROM DM_DEVICE d " +
                    "WHERE d.DEVICE_IDENTIFICATION IN (");

            // adding arguments to the sql query
            Iterator iterator = deviceIds.iterator();
            while (iterator.hasNext()) {
                iterator.next();
                sql.append(" ?");
                if (iterator.hasNext()) {
                    sql.append(",");
                }
            }
            sql.append(") AND d.TENANT_ID = ?) AND e.STATUS = ? AND e.TENANT_ID = ?");

            stmt = conn.prepareStatement(sql.toString());
            int index = 1;
            for (DeviceIdentifier id : deviceIds) {
                stmt.setString(index++, id.getId());
            }
            stmt.setInt(index++, tenantId);
            stmt.setString(index++, status.toString());
            stmt.setInt(index, tenantId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                enrolments.add(DeviceManagementDAOUtil.loadEnrolment(rs));
            }
            return enrolments;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while retrieving the enrolment " +
                    "ids of devices", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    public List<Device> getDevicesByStatus(EnrolmentInfo.Status status, int tenantId)
            throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Device> devices = new ArrayList<>();
        try {
            conn = this.getConnection();
            String sql = "SELECT d.ID AS DEVICE_ID, d.DESCRIPTION, d.NAME AS DEVICE_NAME, t.NAME AS DEVICE_TYPE, " +
                    "d.DEVICE_IDENTIFICATION, e.OWNER, e.OWNERSHIP, e.STATUS, e.DATE_OF_LAST_UPDATE, " +
                    "e.DATE_OF_ENROLMENT FROM (SELECT e.ID, e.DEVICE_ID, e.OWNER, e.OWNERSHIP, e.STATUS, " +
                    "e.DATE_OF_ENROLMENT, e.DATE_OF_LAST_UPDATE, e.ID AS ENROLMENT_ID FROM DM_ENROLMENT e " +
                    "WHERE TENANT_ID = ? AND STATUS = ?) e, DM_DEVICE d, DM_DEVICE_TYPE t " +
                    "WHERE DEVICE_ID = e.DEVICE_ID AND d.DEVICE_TYPE_ID = t.ID AND d.TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setString(2, status.toString());
            stmt.setInt(3, tenantId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Device device = DeviceManagementDAOUtil.loadDevice(rs);
                devices.add(device);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the list of devices that matches to status " +
                    "'" + status + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return devices;
    }

    @Override
    public List<DeviceType> getDeviceTypes()
            throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<DeviceType> deviceTypes;
        try {
            conn = this.getConnection();
            String sql = "SELECT t.ID, t.NAME FROM DM_DEVICE_TYPE t";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            deviceTypes = new ArrayList<>();
            while (rs.next()) {
                DeviceType deviceType = DeviceManagementDAOUtil.loadDeviceType(rs);
                deviceTypes.add(deviceType);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while listing device types.", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
        return deviceTypes;
    }

}
