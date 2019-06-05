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

		main (["switch"])
		details(["switch", "configure", "refresh"])
	}
}

// parse the raw command, pass the command to the correct event handler
def parse(String description) {
	def result = null;
    // 0x20 = Basic, 0x30 = Sensor Binary, 0x70 = Configuration, 
	def cmd = zwave.parse(description, [0x20: 1, 0x30: 1, 0x70: 1]);

	if (cmd) {
		result = createEvent(zwaveEvent(cmd));
	}
	log.debug "Parse returned ${result?.descriptionText} $cmd.CMD";
	return result;
}

// handle binary switch report to update device state in SmartThings
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) { 
	log.debug "MIMOlite sent a switchBinaryReport ${cmd}";
    return [name: "switch", value: cmd.value ? "on" : "off"];
}

// handle binary sensor report to update device state in SmartThings
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	log.debug "MIMOlite sent a sensorBinaryReport command";
	return [name: "contact", value: cmd.sensorValue ? "open" : "closed"];
}

// log unhandled commands
def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug("MIMOlite sent un-parsed Z-Wave message ${cmd}");
	return [:];
}

def configure() {
    log.debug "Sending config commands....";

	def secondsToBuzz = 3;
	def centisecondsToBuzz = (secondsToBuzz*10).toInteger();
    
	delayBetween([
    	// Add SmartThings Hub to Association Group 4. MIMOlite will send a Binary Sensor report to these devices when input is triggered
        zwave.associationV1.associationSet(groupingIdentifier:4, nodeId:[zwaveHubNodeId]).format(),
        // Set number of seconds to engage relay
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
    
    // read state of switch and sensor
	delayBetween([
        zwave.switchBinaryV1.switchBinaryGet().format(),
        zwave.sensorBinaryV2.sensorBinaryGet().format(),
    ]);
}
