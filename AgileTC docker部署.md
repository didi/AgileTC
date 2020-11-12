###### 关于改动的地方

- 修改了Agile-TC github上 spring的配置文件,将数据库相关的配置抽取到了环境变量中.
- 修改了数据库初始化sql文件目录位置,放到了 classpath 根目录下.并在spring的配置文件中将schema指向了初始化sql文件.
- mysql镜像直接引用的官方的,case-server镜像是将 Agile-TC 项目打成jar包 并基于 openjdk8 打成了镜像上传到了我个人的docker-hub仓库中.(也就是说后续更新的话case-server镜像需要同步更新)

###### 如何启动

- 首先要确定本地有docker环境并安装了docker-compose.
- 然后将agile.env 文件和 docker-compose.yml 文件放在同级目录下 docker-compose up 启动即可.



###### 一 .env 文件

```
vim agile.env

MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DATABASE=case_manager
MYSQL_USER=agile
MYSQL_PASSWORD=agile
MYSQL_ROOT_PASSWORD=agile
TZ=Asia/Shanghai
```

###### 二. docker-compose.yml 文件
```
vim docker-compose.yml

version: '3'
services:
  case-server:
    image: yestodayhadrain/case-server:latest
    container_name: agileTC-caseserver
    env_file:
      - ./agile.env
    command: bash -c "cd /app/ && java -jar case-server-1.0-SNAPSHOT.jar"
    # docker 端口映射,如果宿主机 8080 端口被占用需要更改
    ports:
      - "8080:8094"
    depends_on:
      - mysql
    restart: always
    networks:
      - agile-net
  mysql:
    image: mysql:latest
    container_name: agileTC-mysql
    # 挂载到宿主机目录 /data/mysql/data 
    volumes:
      - /data/mysql/data:/var/lib/mysql
    env_file:
      - ./agile.env
    # docker 端口映射,如果宿主机 6666 端口被占用需要更改
    ports:
      - "6666:3306" 
    restart: always
    networks:
      - agile-net
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
networks:
  agile-net:
    driver: bridge
```
