package com.github.serezhka.jap2lib;

import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

class RTSP {

    private static final Logger log = LoggerFactory.getLogger(RTSP.class);

    private String streamConnectionID;
    private byte[] encryptedAESKey;
    private byte[] eiv;

    void rtspSetup(InputStream in, OutputStream out,
                   int videoDataPort, int videoEventPort, int videoTimingPort, int audioDataPort, int audioControlPort) throws Exception {
        NSDictionary request = (NSDictionary) BinaryPropertyListParser.parse(in);

        log.debug("Binary property list parsed:\n{}", request.toXMLPropertyList());

        if (request.containsKey("ekey")) {
            encryptedAESKey = (byte[]) request.get("ekey").toJavaObject();
        }

        if (request.containsKey("eiv")) {
            eiv = (byte[]) request.get("eiv").toJavaObject();
        }

        if (request.containsKey("streams")) {
            HashMap stream = (HashMap) ((Object[]) request.get("streams").toJavaObject())[0]; // iter

            int type = (int) stream.get("type");

            switch (type) {

                // mirror
                case 110: {
                    streamConnectionID = Long.toUnsignedString((long) stream.get("streamConnectionID"));

                    NSArray streams = new NSArray(1);
                    NSDictionary dataStream = new NSDictionary();
                    dataStream.put("dataPort", videoDataPort);
                    dataStream.put("type", 110);
                    streams.setValue(0, dataStream);

                    NSDictionary response = new NSDictionary();
                    response.put("streams", streams);
                    response.put("eventPort", videoEventPort);
                    response.put("timingPort", videoTimingPort);
                    BinaryPropertyListWriter.write(out, response);
                    break;
                }

                // audio
                case 96: {

                    log.debug("Audio format: {}", getAudioFormatDescription((int) stream.get("audioFormat")));

                    NSArray streams = new NSArray(1);
                    NSDictionary dataStream = new NSDictionary();
                    dataStream.put("dataPort", audioDataPort);
                    dataStream.put("type", 96);
                    dataStream.put("controlPort", audioControlPort);
                    streams.setValue(0, dataStream);

                    NSDictionary response = new NSDictionary();
                    response.put("streams", streams);
                    BinaryPropertyListWriter.write(out, response);
                    break;
                }

                default:
                    log.warn("Unknown stream type: {}", type);
            }
        }
    }

    String getStreamConnectionID() {
        return streamConnectionID;
    }

    byte[] getEncryptedAESKey() {
        return encryptedAESKey;
    }

    byte[] getEiv() {
        return eiv;
    }

    private String getAudioFormatDescription(int format) {
        String formatDescription;
        switch (format) {
            case 0x40000:
                formatDescription = "96 AppleLossless, 96 352 0 16 40 10 14 2 255 0 0 44100";
                break;
            case 0x400000:
                formatDescription = "96 mpeg4-generic/44100/2, 96 mode=AAC-main; constantDuration=1024";
                break;
            case 0x1000000:
                formatDescription = "96 mpeg4-generic/44100/2, 96 mode=AAC-eld; constantDuration=480";
                break;
            default:
                formatDescription = "Unknown: " + format;
        }
        return formatDescription;
    }
}
