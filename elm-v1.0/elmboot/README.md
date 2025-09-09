## 配置项目注意事项

1. 使用**jdk-11**（检查环境变量与IDE的配置）。
2. 使用本仓库内的**pom.xml**。
3. 安装**Maven**并使用下面的配置文件（C://Users//User//.m2//settings.xml）：

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">

    <!-- 本地仓库路径（默认是 ${user.home}/.m2/repository） -->
    <localRepository>${user.home}/.m2/repository</localRepository>

    <!-- 镜像配置（加速依赖下载） -->
    <mirrors>
        <!-- 阿里云中央仓库 -->
        <mirror>
            <id>aliyun-central</id>
            <mirrorOf>central</mirrorOf>
            <name>阿里云中央仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </mirror>
        
        <!-- 阿里云公共仓库（包含Spring等常用库） -->
        <mirror>
            <id>aliyun-public</id>
            <mirrorOf>public</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
        
        <!-- 阿里云Spring仓库 -->
        <mirror>
            <id>aliyun-spring</id>
            <mirrorOf>spring</mirrorOf>
            <name>阿里云Spring仓库</name>
            <url>https://maven.aliyun.com/repository/spring</url>
        </mirror>
    </mirrors>

    <!-- 配置默认的JDK编译版本 -->
    <profiles>
        <profile>
            <id>default-jdk</id>
            <activation>
                <activeByDefault>true</activeByDefault>
                <jdk>11</jdk>
            </activation>
            <properties>
                <maven.compiler.source>11</maven.compiler.source>
                <maven.compiler.target>11</maven.compiler.target>
                <maven.compiler.compilerVersion>11</maven.compiler.compilerVersion>
            </properties>
        </profile>
    </profiles>

    <!-- 激活上面定义的profile -->
    <activeProfiles>
        <activeProfile>default-jdk</activeProfile>
    </activeProfiles>

</settings>
```

配置完毕后，执行下列操作（elmboot目录下）：

```
mvn clean install
mvn package
java -jar ./target/elmboot-0.0.1-SNAPSHOT.jar
```

