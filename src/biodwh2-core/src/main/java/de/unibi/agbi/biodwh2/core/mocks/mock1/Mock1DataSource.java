package de.unibi.agbi.biodwh2.core.mocks.mock1;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.*;
import de.unibi.agbi.biodwh2.core.mocks.mock1.etl.*;

public class Mock1DataSource extends DataSource {
    @Override
    public String getId() {
        return "Mock1";
    }

    @Override
    public Updater<Mock1DataSource> getUpdater() {
        return new Mock1Updater();
    }

    @Override
    public Parser<Mock1DataSource> getParser() {
        return new Mock1Parser();
    }

    @Override
    public GraphExporter<Mock1DataSource> getGraphExporter() {
        return new Mock1GraphExporter();
    }

    @Override
    public MappingDescriber getMappingDescriber() {
        return new Mock1MappingDescriber();
    }

    @Override
    protected void unloadData() {
    }
}
