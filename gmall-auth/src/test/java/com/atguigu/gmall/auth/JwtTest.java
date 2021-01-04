package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
    private static final String pubKeyPath = "D:\\develop\\Code\\gulishangcheng\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\develop\\Code\\gulishangcheng\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDkzNzczNDF9.K1QQhiS0JIC3ZJCMy7itWy6YEBHnmsMK-mGgEaH-oNhGSiTRA6mAeKc7mP5oTaeFSL7HkHTtBvuOi_aTcYusmZWJsQJDVngCX3SlkeAG1irGhnz1862k6Otdo2AhlBr7jkuBgtfJ14QjO7k01S3wPbYCRvH3rwSoxplqp5GmYF--hvj6sveVW9Pg36edC942QQ3SQxEAWam3OIaeme8LYOh0EGgIc4QP8dq-cr9-L87UlinrjucnNvAsn96eiD0tcbYcW1YOcLkyZgOtuvJHBg967482xNzVlewZNicsfSpyltPSOdRNjbNQT9sGCwWOYvCRP4_j6TwFRfFt-DJlIQ";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}

