package ca.pfv.spmf.test;

import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.AlgoSPAM;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.creators.AbstractionCreator;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.creators.AbstractionCreator_Qualitative;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.idLists.creators.IdListCreator;
import ca.pfv.spmf.algorithms.sequentialpatterns.spade_spam_AGP.idLists.creators.IdListCreator_FatBitmap;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Example of how to use the algorithm SPAM, saving the results in the 
 * main memory
 *
 * @author agomariz
 */
public class MainTestSPAM_AGP_FatBitMap_saveToFile {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // Load a sequence database
        double support = 0.5;

        boolean keepPatterns = true;
        boolean verbose = false;

        AbstractionCreator abstractionCreator = AbstractionCreator_Qualitative.getInstance();

        IdListCreator idListCreator = IdListCreator_FatBitmap.getInstance();

        SequenceDatabase sequenceDatabase = new SequenceDatabase(abstractionCreator, idListCreator);

        sequenceDatabase.loadFile(fileToPath("contextPrefixSpan.txt"), support);

        System.out.println(sequenceDatabase.toString());

        AlgoSPAM algorithm = new AlgoSPAM(support);

        System.out.println("Minimum absolute support = " + support);
        algorithm.runAlgorithm(sequenceDatabase, keepPatterns, verbose, "/home/agomariz/SPAM_FATBITMAP.txt");
        System.out.println(algorithm.getNumberOfFrequentPatterns() + " frequent patterns.");

        System.out.println(algorithm.printStatistics());
    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestSPADE_AGP_FatBitMap_saveToFile.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }
}
