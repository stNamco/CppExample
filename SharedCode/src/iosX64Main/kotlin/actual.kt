package com.stnamco.shared

import platform.UIKit.UIDevice

import cocoapods.AFNetworking.*

actual class Platform actual constructor() {
    actual fun platformName(): String {
        return UIDevice.currentDevice.systemName()
    }
}