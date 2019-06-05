/**
 *  MIMOlite Z-wave Apartment Buzzer
 *
 *  Device handler for Fortrezz Mimolite, closes the relay for 3 seconds to simulate 
 *  pressing the door button on a standard apartment intercom system (door buzzer)
 *
 *  Copyright 2019 Chris Whong
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Based on Fortrezz MIMOlite Device Handler
 *  https://github.com/fortrezz/smartthings/blob/master/mimolite/devicehandler.groovy
 */

metadata {
	// Automatically generated. Make future change here.
	definition (name: "MIMOlite Door Buzzer", namespace: "fortrezz", author: "Chris Whong") {
        capability "Configuration"
        capability "Switch"
        capability "Refresh"
        capability "Contact Sensor"
        capability "Voltage Measurement"

        command "on"

        fingerprint deviceId: "0x1000", inClusters: "0x72,0x86,0x71,0x30,0x31,0x35,0x70,0x85,0x25,0x03"
	}

	// UI tile definitions 
	tiles (scale: 2) {
        standardTile("switch", "device.switch", width: 6, height: 6) {
            state "on", label: "BZZZZ", icon: "st.locks.lock.unlocked", backgroundColor: "#53a7c0"
			state "off", label: 'Buzz', action: "on", icon: "st.locks.lock.locked", backgroundColor: "#ffffff"
        }
        
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		standardTile("configure", "device.configure", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        valueTile("voltage", "device.voltage", width: 2, height: 2) {
            state "val", label:'${currentValue}v', unit:"", defaultState: true
        }
        valueTile("voltageCounts", "device.voltageCounts", width: 2, height: 2) {
            state "val", label:'${currentValue}', unit:"", defaultState: true
        }

		main (["switch"])
		details(["switch", "configure", "refresh", "voltage"])
	}
}

def parse(String description) {
	def result = null;
	def cmd = zwave.parse(description, [0x20: 1, 0x84: 1, 0x30: 1, 0x70: 1, 0x31: 5]);

	if (cmd) {
		result = createEvent(zwaveEvent(cmd));
	}
	log.debug "Parse returned ${result?.descriptionText} $cmd.CMD";
	return result;
}

def updated() {
	log.debug "Settings Updated..."
    configure();
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) { 
log.debug "MIMOlite sent a switchBinaryReport ${cmd}"
    if (cmd.value) {
		return [name: "switch", value: "on"];
    } else {
		return [name: "switch", value: "off"];
    }      
}
    
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	log.debug "MIMOlite sent a sensorBinaryReport command";
	return [name: "contact", value: cmd.sensorValue ? "open" : "closed"];
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug("MIMOlite sent an-parsed Z-Wave message ${cmd}");
	return [:];
}

def CalculateVoltage(ADCvalue) {
	def map = [:]
     
 	def volt = (((1.5338*(10**-16))*(ADCvalue**5)) - ((1.2630*(10**-12))*(ADCvalue**4)) + ((3.8111*(10**-9))*(ADCvalue**3)) - ((4.7739*(10**-6))*(ADCvalue**2)) + ((2.8558*(10**-3))*(ADCvalue)) - (2.2721*(10**-2)))
	def voltResult = volt.round(1);
    
	map.name = "voltage";
    map.value = voltResult;
    map.unit = "v";
    
    return map;
}
	

def configure() {
    log.debug "Sending config commands....";

	def secondsToBuzz = 3;
	def centisecondsToBuzz = (secondsToBuzz*10).toInteger();
    
	delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier:4, nodeId:[zwaveHubNodeId]).format(),
        zwave.configurationV1.configurationSet(configurationValue: [centisecondsToBuzz], parameterNumber: 11, size: 1).format(),
	]);
}

def on() {
	log.debug "Sending On Command..."
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(), 
        refresh(),
	]);
}

def refresh() {
	log.debug "Refreshing state..."
	delayBetween([
        zwave.switchBinaryV1.switchBinaryGet().format(),
        zwave.sensorMultilevelV5.sensorMultilevelGet().format(),
    ]);
}
