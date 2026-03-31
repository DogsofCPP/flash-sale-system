package com.example.userservice.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/test")
public class DataSourceTestController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/datasource")
    public ResponseEntity<Map<String, Object>> testDataSource() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            result.put("url", metaData.getURL());
            result.put("username", metaData.getUserName());
            result.put("driverName", metaData.getDriverName());
            result.put("message", "数据源连接成功");
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/master")
    public ResponseEntity<Map<String, Object>> testMaster() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String url = metaData.getURL();
            boolean isMaster = url.contains("3307");
            result.put("url", url);
            result.put("isMaster", isMaster);
            result.put("message", isMaster ? "当前使用主库(写)" : "未连接到主库");
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/slave")
    public ResponseEntity<Map<String, Object>> testSlave() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String url = metaData.getURL();
            boolean isSlave = url.contains("3308");
            result.put("url", url);
            result.put("isSlave", isSlave);
            result.put("message", isSlave ? "当前使用从库(读)" : "未连接到从库");
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/read-write")
    public ResponseEntity<Map<String, Object>> testReadWrite() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "读写分离测试接口");
        result.put("endpoints", new String[]{
            "GET /api/test/master - 测试主库连接",
            "GET /api/test/slave - 测试从库连接",
            "GET /api/products - 测试读操作(走从库)",
            "POST /api/products - 测试写操作(走主库)"
        });
        return ResponseEntity.ok(result);
    }
}
