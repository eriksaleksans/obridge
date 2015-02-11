package org.obridge;

import com.thoughtworks.xstream.XStream;
import java.beans.PropertyVetoException;
import org.obridge.context.OBridgeConfiguration;
import org.obridge.generators.ConverterObjectGenerator;
import org.obridge.generators.EntityObjectGenerator;
import org.obridge.generators.PackageObjectGenerator;
import org.obridge.generators.ProcedureContextGenerator;
import org.obridge.util.XStreamFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Hello world!
 */
public class OBridge {

    public void generate(OBridgeConfiguration c) throws SQLException, IOException, PropertyVetoException {
        // generate objects
        EntityObjectGenerator.generate(c);

        // generate converters
        ConverterObjectGenerator.generate(c);

        // generate contexts
        ProcedureContextGenerator.generate(c);

        // generate packages
        PackageObjectGenerator.generate(c);

    }

    public OBridgeConfiguration loadConfiguration(File f) {
        XStream xs = XStreamFactory.createXStream();
        Object config = xs.fromXML(f);
        return (OBridgeConfiguration) config;
    }

    public void generate(File f) throws IOException, SQLException, PropertyVetoException {
        this.generate(loadConfiguration(f));
    }

    public static void main(String[] args) throws SQLException, IOException, PropertyVetoException {
        new OBridge().generate(new File(args[0]));
    }
}
