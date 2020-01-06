#### 乐亭项目RFID读取

**安装方式**
`cordova plugin add `

**相关方法**

 - openBlueTooth
     - 参数 
         - success 成功回调
         - error 失败回调 
     - 返回值
         - success => 蓝牙开启成功
         - error => 蓝牙开启失败
     - 调用示例
         ```
           rfid.openBlueTooth(success,error);
         ```
         
 - getBlueToothList
    - 参数 
         - success 成功回调
         - error 失败回调 
     - 返回值
         - success => { message: "", data:[ 蓝牙设备list ] }  
         - error => errorMsg
     - 调用示例
         ```
            rfid.getBlueToothList(success,error);
         ```
 - connectBlueTooth
     - 参数 
         - success 成功回调
         - error 失败回调 
         - address 待链接蓝牙地址
     - 返回值
         - success => 模块打开成功
         - error => 模块打开失败
     - 调用示例
         ```
            rfid.connectBlueTooth(success,error,{address: address});
         ```
 - read
    - 参数 
         - success 成功回调
         - error 失败回调 
     - 返回值
         - success => { message: "", data:[ EPClist ] }  
         - error => errorMsg
     - 调用示例
         ```
            rfid.read(success,error);
         ```
 - write
     - 参数 
         - success 成功回调
         - error 失败回调 
         - epc_number 待写入EPC值
         - epc_len EPC编码方式，16进制 **选填，默认0c**
     - 返回值
         - success => 写入成功
         - error => errorMsg
     - 调用示例
         ```
            rfid.write(success,error, {epc_number: $scope.input.epc_number});
         ```