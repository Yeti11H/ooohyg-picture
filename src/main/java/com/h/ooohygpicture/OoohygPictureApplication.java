package com.h.ooohygpicture;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.h.ooohygpicture.mapper")
@EnableAspectJAutoProxy(exposeProxy  = true)
@EnableTransactionManagement // 👈 加上这个
public class OoohygPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(OoohygPictureApplication.class, args);
    }

}
