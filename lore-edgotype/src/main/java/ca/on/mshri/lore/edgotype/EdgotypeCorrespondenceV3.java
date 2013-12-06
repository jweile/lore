package ca.on.mshri.lore.edgotype;

import ca.on.mshri.lore.genome.Allele;
import ca.on.mshri.lore.genome.Gene;
import ca.on.mshri.lore.interaction.InteractionModel;
import ca.on.mshri.lore.interaction.PhysicalInteraction;
import ca.on.mshri.lore.molecules.Molecule;
import ca.on.mshri.lore.molecules.Protein;
import ca.on.mshri.lore.operations.LoreOperation;
import ca.on.mshri.lore.phenotype.Phenotype;
import ca.on.mshri.lore.phenotype.PhenotypeModel;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Finds a list of genes for which there is a correspondence between
 * edgotypes and disease annotations for all its alleles.
 * 
 * @author Jochen Weile <jochenweile@gmail.com>com
 */
public class EdgotypeCorrespondenceV3 extends LoreOperation {

    private Property affectsPositively, affectsNegatively, isCausallyAssociatedWith, encBy;
    private PhenotypeModel phenoModel;
    
    void init() {
        phenoModel = new PhenotypeModel(OntModelSpec.OWL_MEM, getModel());
        affectsNegatively = phenoModel.getProperty(InteractionModel.URI+"#affectsNegatively");
        affectsPositively = phenoModel.getProperty(InteractionModel.URI+"#affectsPositively");
        isCausallyAssociatedWith = phenoModel.getProperty(PhenotypeModel.URI+"#isCausallyAssociatedWith");
    }
    
    @Override
    public void run() {

        init();
        
        int sameDsameE = 0, sameDdiffE = 0, diffDsameE = 0, diffDdiffE = 0;
        
        //iterate over all genes
        List<Gene> genes = phenoModel.listIndividualsOfClass(Gene.class, false);
        geneloop: for (Gene gene : genes) {
            //a map for indexing sets of alleles according to edgotype signatures
            //            LazyInitMap<String,Set<Allele>> edgotypeIndex = new LazyInitMap<String, Set<Allele>>(HashSet.class);
            
            
            //get all alleles of the gene
            List<Allele> alleles = gene.listAlleles();
            //eliminate genes without alleles
            if (alleles.isEmpty()) {
                continue;
            }
            
            //index edgotypes for alleles
            Map<Allele,Set<String>> edgotypes = new HashMap<Allele,Set<String>>();
            Map<Allele,String> edgotypeClasses = new HashMap<Allele,String>();
            for (Allele allele : alleles) {
                Set<String> edgotype = edgotype(allele, gene);
                //skip allels without diseases or edgotypes
                if (edgotype.isEmpty() || diseases(allele).isEmpty()) {
                    continue;
                }
                edgotypes.put(allele,edgotype);
                
                int pos = 0, neg = 0;
                for (String iStatus : edgotype) {
                    if (iStatus.startsWith("pos")) {
                        pos++;
                    } else {
                        neg++;
                    }
                }
                if (pos == 0) {
                    edgotypeClasses.put(allele, "PN");
                } else if (neg == 0) {
                    edgotypeClasses.put(allele, "PW");
                } else {
                    edgotypeClasses.put(allele, "E");
                }
            }
            //skip genes without edgotypes
            if (edgotypes.isEmpty()) {
                continue geneloop;
            }
            //filter out proteins with degree < 2
            if (edgotypes.values().iterator().next().size() < 2) {
                continue geneloop;
            }
            
            //genes must be pleiotropic
            //skip genes with only one disease annotation
            Set<Phenotype> diseases = new HashSet<Phenotype>();
            for (Allele allele : alleles) { 
                diseases.addAll(diseases(allele));
            }
            if (diseases.size() < 2) {
                continue;
            }
            
            
            List<Allele> as = new ArrayList<Allele>(alleles);
            for (int i = 1; i < as.size(); i++) {
                Allele a_i = as.get(i);
                if (edgotypes.get(a_i) == null) {
                    continue;
                }
                for (int j = 0; j < i; j++) {
                    Allele a_j = as.get(j);
                    
                    if (edgotypes.get(a_j) == null) {
                        continue;
                    }
                    double diseaseSimilarity = jaccard(diseases(a_i), diseases(a_j));
//                    double edgotypeSimilarity = 0;
//                    try {
//                        edgotypeSimilarity = setSim(edgotypes.get(a_i), edgotypes.get(a_j));
//                    } catch (Exception e) {
//                        Logger.getLogger(EdgotypeCorrespondenceV3.class.getName())
//                                .log(Level.WARNING, "Incomparable edgotypes! "+a_i.toString()+" "+a_j.toString());
//                        continue geneloop;
//                    }
                    
                    if (diseaseSimilarity > 0) {
                        if (edgotypeClasses.get(a_i).equals(edgotypeClasses.get(a_j))) {
                            sameDsameE++;
                        } else {
                            sameDdiffE++;
                        }
                    } else {
                        if (edgotypeClasses.get(a_i).equals(edgotypeClasses.get(a_j))) {
                            diffDsameE++;
                        } else {
                            diffDdiffE++;
                        }
                    }
                }
            }
            
            
//            //alleles with same edgotype must have similar diseases
//            for (String edgotype : edgotypeIndex.keySet()) {
//                //get alleles with same edgotype
//                Set<Allele> as = edgotypeIndex.get(edgotype);
//                if (as.size() > 1) {
//                    double[] sims = diseaseSimilarities(as);
//                    if (hasZero(sims)) {
//                        //skip this gene
//                        continue geneloop;
//                    }
//                }
//            }
//            
//            //alleles with different edgotype should have different diseases
//            Set<Allele> repAlleles = new HashSet<Allele>();
//            for (String edgotype : edgotypeIndex.keySet()) {
//                Allele repAllele = edgotypeIndex.get(edgotype).iterator().next();
//                repAlleles.add(repAllele);
//            }
//            if (repAlleles.size() > 1) {
//                double[] sims = diseaseSimilarities(repAlleles);
//                if (!allZero(sims)) {
//                    //skip this gene
//                    continue geneloop;
//                }
//            }
//            
//            //if we get to here we have fulfilled all criteria
//            String entrez = gene.getXRefValue(phenoModel.ENTREZ);
//            String symbol = gene.getXRefValue(phenoModel.HGNC);
//            System.out.println(symbol+" (Entrez:"+entrez+")");
//            
//            for (String edgotype : edgotypeIndex.keySet()) {
//                System.out.println("  -> Edgotype: "+edgotype);
//                for (Allele allele : edgotypeIndex.get(edgotype)) {
//                    String omim = allele.getXRefValue(phenoModel.OMIM);
//                    String alleleId = omim == null ? "HGMD:"+allele.getXRefValue(phenoModel.HGMD) : "OMIM:"+omim;
//                    System.out.println("    -> Allele: "+alleleId);
//                    System.out.println("      -> Diseases: "+formatDiseases(diseases(allele)));
//                }
//            }
            
//            for (Allele allele : gene.listAlleles()) {
//                String omim = allele.getXRefValue(phenoModel.OMIM);
//                String alleleId = omim == null ? "HGMD:"+allele.getXRefValue(phenoModel.HGMD) : "OMIM:"+omim;
//            
//                String mutDesc = getMutationInfo(allele);
//                System.out.println("  - "+alleleId+" ("+mutDesc+")");
//                
//                System.out.println("    * "+formatDiseases(diseases(allele)));
//                System.out.println("    + "+edgotype(allele, gene));
//            }
            
        }
        
        System.out.println("       | Same D | Diff D");
        System.out.print(  "Same E |");
        System.out.print(sameDsameE);
        System.out.print(" | ");
        System.out.println(diffDsameE);
        System.out.print(  "Diff E |");
        System.out.print(sameDdiffE);
        System.out.print(" | ");
        System.out.println(diffDdiffE);


    }

    @Override
    public boolean requiresReasoner() {
        return false;
    }

    /**
     * Retrieves the edgotype profile of the given allele.
     * This is represented as a set of strings which indicate positive or negative effects
     * on the different interactions in which the encoded gene engages.
     */
    private Set<String> edgotype(Allele allele, Gene gene) {
                
        Set<String> set = new TreeSet<String>();
        
        NodeIterator it = allele.listPropertyValues(affectsPositively);
        while (it.hasNext()) {
            PhysicalInteraction ia = PhysicalInteraction.fromIndividual(it.next().as(Individual.class));
            String interactor = getOtherProtein(ia, gene);
            set.add("pos:" + interactor);
        }
        
        it = allele.listPropertyValues(affectsNegatively);
        while (it.hasNext()) {
            PhysicalInteraction ia = PhysicalInteraction.fromIndividual(it.next().as(Individual.class));
            String interactor = getOtherProtein(ia, gene);
            set.add("neg:" + interactor);
        }
        
        return set;
    }

    

    /**
     * Computes the Jaccard coefficent for overlap between two sets.
     */
    private <T> double jaccard(Set<T> a, Set<T> b) {
        Set<T> union = new HashSet<T>(a);
        union.addAll(b);
        
        Set<T> intersection = new HashSet<T>(a);
        intersection.retainAll(b);
        
        return (double)intersection.size() / (double)union.size();
    }

    /**
     * retrieves the set of phenotypes associated with the allele
     * @param allele
     * @return 
     */
    private Set<Phenotype> diseases(Allele allele) {
        Set<Phenotype> set = new HashSet<Phenotype>();
        NodeIterator it = allele.listPropertyValues(isCausallyAssociatedWith);
        while (it.hasNext()) {
            Phenotype disease = Phenotype.fromIndividual(it.next().as(Individual.class));
            set.add(disease);
        }
        return set;
    }

//    /**
//     * computes the similarites between all possible pairs of phenotypeSets 
//     * that are associated with the given alleles.
//     * @param alleles
//     * @return a double array, containing the jaccard coefficients between the disease sets
//     */
//    double[] diseaseSimilarities(Set<Allele> alleles) {
//        List<Allele> as = new ArrayList<Allele>(alleles);
//        int n = as.size();
//        double[] sims = new double[(n*n - n) / 2];
//        int k = 0;
//        for (int i = 1; i < as.size(); i++) {
//            Allele a_i = as.get(i);
//            for (int j = 0; j < i; j++) {
//                Allele a_j = as.get(j);
//                sims[k++] = jaccard(diseases(a_i), diseases(a_j));
//            }
//        }
//        return sims;
//    }

//    boolean hasZero(double[] sims) {
//        boolean zero = false;
//        for (double sim : sims) {
//            zero |= sim == 0.0;
//        }
//        return zero;
//    }
//
//    boolean allZero(double[] sims) {
//        boolean allZero = true;
//        for (double sim :sims) {
//            allZero &= sim == 0;
//        }
//        return allZero;
//    }
//
//    String getMutationInfo(Allele allele) {
//        List<Mutation> muts = allele.listMutations();
//        if (muts != null && !muts.isEmpty()) {
//            PointMutation pmut = PointMutation.fromIndividual(muts.get(0));
//            String desc = pmut.getFromAminoAcid()+pmut.getPosition()+pmut.getToAminoAcid();
//            return desc;
//        }
//        return "";
//    }

    String getOtherProtein(PhysicalInteraction ia, Gene gene) {
        List<Molecule> mols = ia.listParticipants();
        Molecule same = null;
        for (Molecule mol : mols) {
            Protein protein = Protein.fromIndividual(mol);
            if (protein.getEncodingGene() != null && protein.getEncodingGene().equals(gene)) {
                same = mol;
            }
        }
        mols.remove(same);
        List<String> ids  = new ArrayList<String>();
        for (Molecule mol : mols) {
            ids.add(mol.getXRefValue(phenoModel.ENTREZ));
        }
        if (ids.size() == 1) {
            return ids.get(0);
        } else {
            return ids.toString();
        }
    }

//    private String formatDiseases(Set<Phenotype> diseases) {
//        
//        if (diseases.isEmpty()) {
//            return "N/A";
//        }
//        
//        StringBuilder b = new StringBuilder();
//        
//        for (Phenotype disease : diseases) {
//            String bestLabel = null;
//            int maxLength = 0;
//            ExtendedIterator<RDFNode> it = disease.listLabels(null);
//            while (it.hasNext()) {
//                String label = it.next().asLiteral().getString();
//                if (label.length() > maxLength) {
//                    maxLength = label.length();
//                    bestLabel = label;
//                }
//            }
//            
//            String omim = disease.getXRefValue(phenoModel.OMIM);
//            String id = omim == null ? "HGMD:"+disease.getXRefValue(phenoModel.HGMD) : "OMIM:"+omim;
//            
//            b.append(bestLabel)
//                    .append(" (")
//                    .append(id)
//                    .append("), ");
//            
//        }
//        
//        b.delete(b.length() -2, b.length());
//        
//        return b.toString();
//    }

    private double setSim(Set<String> a, Set<String> b) {
        if (a.size() != b.size()) {
            throw new RuntimeException("Incomparable edgotypes!");
        }
        List<String> aList = new ArrayList<String>(a);
        List<String> bList = new ArrayList<String>(b);
        int matches = 0;
        for (int i = 0; i < a.size(); i++) {
            if (aList.get(i).equals(bList.get(i))) {
                matches++;
            }
        }
        return (double)matches / a.size();
    }

    
}