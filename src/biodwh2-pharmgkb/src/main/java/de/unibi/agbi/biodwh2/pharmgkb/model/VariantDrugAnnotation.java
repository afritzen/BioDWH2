package de.unibi.agbi.biodwh2.pharmgkb.model;

import com.univocity.parsers.annotations.Parsed;
import de.unibi.agbi.biodwh2.core.model.graph.GraphProperty;
import de.unibi.agbi.biodwh2.core.model.graph.NodeLabel;

@NodeLabel("VariantDrugAnnotation")
public class VariantDrugAnnotation {
    @Parsed(field = "Annotation ID")
    @GraphProperty("id")
    public Integer annotationId;
    @Parsed(field = "Variant")
    public String variant;
    @Parsed(field = "Gene")
    public String gene;
    @Parsed(field = "Chemical")
    public String chemical;
    @Parsed(field = "PMID")
    @GraphProperty("pmid")
    public String pmid;
    @Parsed(field = "Phenotype Category")
    public String phenotypeCategory;
    @Parsed(field = "Significance")
    @GraphProperty("significance")
    public String significance;
    @Parsed(field = "Notes")
    @GraphProperty("notes")
    public String notes;
    @Parsed(field = "Sentence")
    @GraphProperty("sentence")
    public String sentence;
    @Parsed(field = "StudyParameters")
    public String studyParameters;
    @Parsed(field = "Alleles")
    @GraphProperty("alleles")
    public String alleles;
    @Parsed(field = "Chromosome")
    @GraphProperty("chromosome")
    public String chromosome;
}
