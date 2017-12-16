package com.vr_object.fixed.xnzrw24b;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class UsbSerialPortTi {
    private final String TAG = UsbSerialPortTi.class.getSimpleName();
    private Context context;
    private UsbManager usbManager;
    UsbDevice device = null;
    UsbDeviceConnection connection = null;
    private UsbInterface usbInterface;
    private UsbEndpoint readEndpoint;
    private UsbEndpoint mWriteEndpoint;
    private boolean enableAsyncReads = false;

    private static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 1024;
    private byte[] readBuffer;
    private byte[] writeBuffer;
    private final Object readBufferLock = new Object();
    private final Object writeBufferLock = new Object();


    public UsbSerialPortTi(Context context){
        this.context = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        enableAsyncReads = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1);
        readBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        writeBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
    }

    public void init() throws DeviceNotFoundException, DeviceOpenFailedException {
        findDevice();
        open();
    }

    public void close() {
        if ( connection != null ) {
            connection.releaseInterface(usbInterface);
            connection.close();
        }
    }

    private void findDevice() throws DeviceNotFoundException {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        for (final UsbDevice usbDevice : usbDevices.values()) {
            final int vendorId  = usbDevice.getVendorId();
            final int productId = usbDevice.getProductId();
            Log.d(TAG,String.format("DEVICE: 0x%04X 0x%04X", vendorId, productId));

            // ble
            if ( vendorId == 0x0451 && productId == 0x16C5 ) {
                device = usbDevice;
                return;
            }

            // wifi
            if ( vendorId == 0x04B4 && productId == 0x0005 ) {
                device = usbDevice;
                return;
            }

            // wifi STM
            if ( vendorId == 0x2341 && productId == 0x0043 ) {
                device = usbDevice;
                return;
            }

            // wifi STM-2 (??? new one)
            if ( vendorId == 0x0483 && productId == 0x5740 ) {
                device = usbDevice;

                //Only while BLE and Wi-Fi devices have same productId!
//                GlobalSettings.setMode(GlobalSettings.WorkMode.WIFI);
//                GlobalSettings.setMode(GlobalSettings.WorkMode.BLE);
                return;
            }

            // fx3
            if ( vendorId == 0x04B4 && productId == 0x00f1 ) {
                device = usbDevice;
                return;
            }

        }
        device = null;
        throw new DeviceNotFoundException();
    }

    private static final String ACTION_USB_PERMISSION = "com.vr_object.fixed.xnzrw24b.USB_PERMISSION";

    private void askPermissions() {
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPermissionIntent);
    }

    private void open() throws DeviceNotFoundException, DeviceOpenFailedException {

        if ( device == null ) {
            findDevice();
        }

        connection = usbManager.openDevice(device);
        if (connection == null ) {
            askPermissions();
            throw new DeviceOpenFailedException();
        }

        Log.d(TAG, String.format("Ifce count = %d", device.getInterfaceCount()));
        for ( int i = 0; i < device.getInterfaceCount(); ++i ) {
            UsbInterface ifce = device.getInterface(i);

            Log.d(TAG, String.format("   Ifce[%d]: class %d",
                    i, ifce.getInterfaceClass()
                    ));

            for ( int k = 0; k < ifce.getEndpointCount(); ++k ) {
                Log.d(TAG, String.format("       EP[%d] dir %d", k, ifce.getEndpoint(k).getDirection()));
            }
        }

        int ifce = 1;
        int rep = 1;
        int wep = 0;
        Log.d(TAG, "Claiming data interface " + String.format( "ifce=%d", ifce ));
        usbInterface = device.getInterface(ifce);
        Log.d(TAG, "data iface=" + usbInterface);


        if (!connection.claimInterface(usbInterface, true)) {
            Log.e(TAG, "claimIfce error");
            throw new DeviceOpenFailedException();
        } else {
            Log.d(TAG, "claimIfce OK");
        }
        Log.d(TAG, "readEndpoint " + String.format( "ep=%d", rep ));
        readEndpoint = usbInterface.getEndpoint(rep);
        Log.d(TAG, "Read endpoint direction: " + readEndpoint.getDirection());
        // Should be UsbConstants.USB_DIR_IN = 0x80 (128 decimal)

        Log.d(TAG, "writeEndpoint " + String.format( "ep=%d", wep ));
        mWriteEndpoint = usbInterface.getEndpoint(wep);
        Log.d(TAG, "Write endpoint direction: " + mWriteEndpoint.getDirection());


        if (enableAsyncReads) {
            Log.d(TAG, "Async reads enabled");
        } else {
            Log.d(TAG, "Async reads disabled.");
        }
    }

    public int write(byte oneChar, int timeoutMillis) throws IOException {
        Log.d( TAG, "write( " + oneChar + " )" );
        byte[] buf = new byte[1];
        buf[0] = oneChar;
        return write(buf, timeoutMillis);
    }

    public int write(byte[] src, int timeoutMillis) throws IOException {
        int offset = 0;

        while (offset < src.length) {
            final int writeLength;
            final int amtWritten;

            synchronized (writeBufferLock) {
                final byte[] buffer;

                writeLength = Math.min(src.length - offset, writeBuffer.length);
                if (offset == 0) {
                    buffer = src;
                } else {
                    // bulkTransfer does not support offsets, make a copy.
                    System.arraycopy(src, offset, writeBuffer, 0, writeLength);
                    buffer = writeBuffer;
                }

                amtWritten = connection.bulkTransfer(mWriteEndpoint, buffer, writeLength,
                        timeoutMillis);
            }
            if (amtWritten <= 0) {
                throw new IOException("Error writing " + writeLength
                        + " bytes at offset " + offset + " length=" + src.length);
            }

            Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
            offset += amtWritten;
        }
        return offset;
    }


    public int read(byte[] dest, int timeoutMillis) throws IOException {
        if (enableAsyncReads) {
            final UsbRequest request = new UsbRequest();
            try {
                request.initialize(connection, readEndpoint);
                final ByteBuffer buf = ByteBuffer.wrap(dest);
                if (!request.queue(buf, dest.length)) {
                    throw new IOException("Error queueing request.");
                }

                final UsbRequest response = connection.requestWait();
                if (response == null) {
                    throw new IOException("Null response");
                }

                final int nread = buf.position();
                if (nread > 0) {
                    //Log.d(TAG, HexDump.dumpHexString(dest, 0, Math.min(32, dest.length)));
                    return nread;
                } else {
                    return 0;
                }
            } finally {
                request.close();
            }
        }

        final int numBytesRead;
        synchronized (readBufferLock) {
            int readAmt = Math.min(dest.length, readBuffer.length);
            numBytesRead = connection.bulkTransfer(readEndpoint, readBuffer, readAmt,
                    timeoutMillis);
            if (numBytesRead < 0) {
                // This sucks: we get -1 on timeout, not 0 as preferred.
                // We *should* use UsbRequest, except it has a bug/api oversight
                // where there is no way to determine the number of bytes read
                // in response :\ -- http://b.android.com/28023
                if (timeoutMillis == Integer.MAX_VALUE) {
                    // Hack: Special case "~infinite timeout" as an error.
                    return -1;
                }
                return 0;
            }
            System.arraycopy(readBuffer, 0, dest, 0, numBytesRead);
        }
        return numBytesRead;
    }

}
