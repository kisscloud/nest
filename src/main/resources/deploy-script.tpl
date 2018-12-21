#!/bin/bash &#x000A;
name=%s &#x000A;
path=%s &#x000A;
rm -fr /opt/apps/$name &#x000A;
mkdir -p /opt/apps/$name &#x000A;
cd /opt/apps/$name &#x000A;
wget __TAR__PACKAGE__ &#x000A;
tar -xvf * &#x000A;
rm -fr /opt/configs/$path &#x000A;
mkdir -p /opt/configs/$path &#x000A;
cd /opt/configs/$path &#x000A;

# 默认的配置文件放在项目的同级项目组的config项目中，如没有,请创建,或者修改配置文件的拉去地址&#x000A;
# 默认配置文件放在团队项目组下 config-server 仓库中，以项目路径命名

# git clone git@git.jincse.com/youbipay/config-server-test
# git clone git@git.jincse.com/youbipay/config-server-staging
# git clone git@git.jincse.com/youbipay/config-server-production

# if [ ! -d "/opt/configs/youbipay/config-server" ];then
#  cd /opt/configs/youbipay
#  git clone git@git.jincse.com:youbipay/config-server-test.git
# else
#  cd /opt/configs/youbipay/config-server-test
#  git pull
# fi


git clone __CONFIG__ &#x000A;

if [ ! -d "/opt/logs/$name" ];then &#x000A;
  mkdir -p /opt/logs/$name &#x000A;
fi &#x000A;