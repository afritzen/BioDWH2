package de.unibi.agbi.biodwh2.disgenet;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.*;
import de.unibi.agbi.biodwh2.disgenet.etl.DisGeNetGraphExporter;
import de.unibi.agbi.biodwh2.disgenet.etl.DisGeNetMappingDescriber;
import de.unibi.agbi.biodwh2.disgenet.etl.DisGeNetParser;
import de.unibi.agbi.biodwh2.disgenet.etl.DisGeNetUpdater;

public class DisGeNetDataSource extends DataSource {
    @Override
    public String getId() {
        return "DisGeNET";
    }

    @Override
    public Updater getUpdater() {
        return new DisGeNetUpdater();
    }

    @Override
    protected Parser getParser() {
        return new DisGeNetParser();
    }

    @Override
    protected GraphExporter getGraphExporter() {
        return new DisGeNetGraphExporter();
    }

    @Override
    public MappingDescriber getMappingDescriber() {
        return new DisGeNetMappingDescriber();
    }

    @Override
    protected void unloadData() {
    }
}
