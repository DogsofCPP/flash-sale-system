package com.example.userservice.web;

import com.example.common.Result;
import com.example.userservice.datasource.DataSource;
import com.example.userservice.datasource.DataSourceContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class DataSourceTestController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/datasource")
    public Result<Map<String, Object>> testDataSource() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            result.put("url", metaData.getURL());
            result.put("driverName", metaData.getDriverName());
            result.put("message", "数据源连接成功");
        }
        return Result.success(result);
    }

    @GetMapping("/master")
    @DataSource("master")
    public Result<Map<String, Object>> testMaster() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            result.put("dataSource", DataSourceContextHolder.getDataSource());
            result.put("message", "当前使用主库(写)");
        }
        return Result.success(result);
    }

    @GetMapping("/slave")
    @DataSource("slave")
    public Result<Map<String, Object>> testSlave() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            result.put("dataSource", DataSourceContextHolder.getDataSource());
            result.put("message", "当前使用从库(读)");
        }
        return Result.success(result);
    }

    @GetMapping("/read-write")
    public Result<Map<String, Object>> testReadWrite() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "读写分离测试接口");
        result.put("endpoints", new String[]{
            "GET /api/test/master - 测试主库连接",
            "GET /api/test/slave - 测试从库连接",
            "GET /api/users - 测试读操作(走从库)",
            "POST /api/users/register - 测试写操作(走主库)"
        });
        return Result.success(result);
    }
}
