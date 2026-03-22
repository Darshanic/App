package com.ksrtc.ticketprinter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothPrinterManager {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
        private set

    // Standard SPP UUID for Bluetooth Serial Port Profile (Printers)
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun connectToPrinter(device: BluetoothDevice): Boolean {
        return try {
            // Cancel discovery to prevent slow connection
            bluetoothAdapter?.cancelDiscovery()

            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Log.d("BTPrinter", "Connected to ${device.name}")
            true
        } catch (e: IOException) {
            Log.e("BTPrinter", "Connection failed", e)
            disconnect()
            false
        }
    }

    fun printData(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("BTPrinter", "Failed to print data", e)
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BTPrinter", "Error closing socket", e)
        } finally {
            outputStream = null
            bluetoothSocket = null
        }
    }
}
