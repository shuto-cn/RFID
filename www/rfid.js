var exec = require('cordova/exec');

var rfid = {
    openBlueTooth: function(success, error) {
        exec(success, error, "rfid", "open_bluetooth", []);
    },
    getBlueToothList: function(success, error) {
        exec(success, error, "rfid", "get_bluetooth_list", []);
    },
    connectBlueTooth: function(success, error, address) {
        exec(success, error, "rfid", "connect_bluetooth", [address]);
    },
    read: function(success, error) {
        exec(success, error, "rfid", "read", []);
    },
    write: function(success, error, epc_number, epc_len) {
        exec(success, error, 'rfid', 'write', [epc_number, epc_len]);
    }
};

module.exports = rfid;