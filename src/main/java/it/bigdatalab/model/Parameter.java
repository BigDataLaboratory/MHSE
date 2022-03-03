package it.bigdatalab.model;

import org.apache.maven.model.Build;

public class Parameter {
    private final String mInputFilePathGraph;
    private final String mOutputFolderPath;
    private final String mAlgorithmName;
    private final int mNumTests;

    private final int mNumSeeds;
    private final boolean mIsSeedsRandom;
    private final boolean mAutomaticRange;
    private final String mInputFilePathSeedNode;
    private final int[] mRange;

    private final boolean mIsolatedVertices;
    private final String mDirection;
    private final boolean mTranspose;

    private final boolean mInMemory;
    private final boolean mComputeCentrality;
    private final boolean mReordering;

    private final double mThreshold;

    private final int mNumThreads;

    private final boolean mPersistCollisionTable;

    private final boolean webGraph;
    private final boolean compGraph;
    private final boolean compEGraph;


    private final boolean differentialCompression;

    @org.jetbrains.annotations.Contract(pure = true)
    public Parameter(Builder builder) {
        this.mInputFilePathGraph = builder.inputFilePathGraph;
        this.mOutputFolderPath = builder.outputFolderPath;
        this.mAlgorithmName = builder.algorithmName;
        this.mNumTests = builder.numTests;

        this.mNumSeeds = builder.numSeeds;
        this.mIsSeedsRandom = builder.isSeedsRandom;
        this.mAutomaticRange = builder.automaticRange;

        this.mInputFilePathSeedNode = builder.inputFilePathSeedNode;
        this.mRange = builder.range;

        this.mIsolatedVertices = builder.isolatedVertices;
        this.mDirection = builder.direction;
        this.mTranspose = builder.transpose;

        this.mInMemory = builder.inMemory;
        this.mComputeCentrality = builder.computeCentrality;
        this.mThreshold = builder.threshold;
        this.mReordering = builder.reordering;

        this.mNumThreads = builder.numThreads;
        this.mPersistCollisionTable = builder.persistCollisionTable;

        this.webGraph = builder.webG;
        this.compGraph = builder.compG;

        this.differentialCompression = builder.differentialCompression;

        this.compEGraph = builder.compEGraph;
    }

    public boolean getDifferentialCompression(){return  differentialCompression;}
    public boolean getWebGraph(){ return  webGraph;}
    public boolean getCompGraph(){ return compGraph;}
    public boolean getCompEGraph(){ return compEGraph;}

    public String getInputFilePathGraph() {
        return mInputFilePathGraph;
    }

    public String getOutputFolderPath() {
        return mOutputFolderPath;
    }

    public String getAlgorithmName() {
        return mAlgorithmName;
    }

    public int getNumTests() {
        return mNumTests;
    }

    public int getNumSeeds() {
        return mNumSeeds;
    }

    public boolean isSeedsRandom() {
        return mIsSeedsRandom;
    }

    public boolean isAutomaticRange() {
        return mAutomaticRange;
    }

    public String getInputFilePathSeedNode() {
        return mInputFilePathSeedNode;
    }

    public int[] getRange() {
        return mRange;
    }

    public boolean keepIsolatedVertices() {
        return mIsolatedVertices;
    }

    public String getDirection() {
        return mDirection;
    }

    public boolean isTranspose() {
        return mTranspose;
    }

    public boolean isInMemory() {
        return mInMemory;
    }

    public boolean computeCentrality() {
        return mComputeCentrality;
    }

    public boolean getReordering() {
        return mReordering;
    }

    public double getThreshold() {
        return mThreshold;
    }

    public int getNumThreads() {
        return mNumThreads;
    }

    public boolean persistCollisionTable() {
        return mPersistCollisionTable;
    }

    public static class Builder {
        public boolean compEGraph;
        private String inputFilePathGraph;
        private String outputFolderPath;
        private String algorithmName;
        private int numTests;

        private int numSeeds;
        private boolean isSeedsRandom;
        private boolean automaticRange;
        private String inputFilePathSeedNode;
        private int[] range;

        private boolean isolatedVertices;
        private String direction;
        private boolean transpose;

        private boolean inMemory;
        private boolean computeCentrality;
        private boolean reordering;
        private double threshold;

        private int numThreads;

        private boolean persistCollisionTable;

        private boolean webG;
        private boolean compG;
        private boolean compEG;

        private boolean differentialCompression;
        public Builder() {
        }

        public Builder setInputFilePathGraph(String inputFilePathGraph) {
            this.inputFilePathGraph = inputFilePathGraph;
            return this;
        }

        public Builder setOutputFolderPath(String outputFolderPath) {
            this.outputFolderPath = outputFolderPath;
            return this;
        }

        public Builder setAlgorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
            return this;
        }

        public Builder setNumTests(int numTests) {
            this.numTests = numTests;
            return this;
        }

        public Builder setNumSeeds(int numSeeds) {
            this.numSeeds = numSeeds;
            return this;
        }

        public Builder setSeedsRandom(boolean isSeedsRandom) {
            this.isSeedsRandom = isSeedsRandom;
            return this;
        }

        public Builder setAutomaticRange(boolean automaticRange) {
            this.automaticRange = automaticRange;
            return this;
        }

        public Builder setInputFilePathSeedNode(String inputFilePathSeedNode) {
            this.inputFilePathSeedNode = inputFilePathSeedNode;
            return this;
        }

        public Builder setRange(int[] range) {
            this.range = range;
            return this;
        }

        public Builder setIsolatedVertices(boolean isolatedVertices) {
            this.isolatedVertices = isolatedVertices;
            return this;
        }

        public Builder setDirection(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder setTranspose(boolean transpose) {
            this.transpose = transpose;
            return this;
        }

        public Builder setReordering(boolean reordering) {
            this.reordering = reordering;
            return this;
        }

        public Builder setInMemory(boolean inMemory) {
            this.inMemory = inMemory;
            return this;
        }

        public Builder setComputeCentrality(boolean computeCentrality) {
            this.computeCentrality = computeCentrality;
            return this;
        }

        public Builder setThreshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder setNumThreads(int numThreads) {
            this.numThreads = numThreads;
            return this;
        }

        public Builder setPersistCollisionTable(boolean persistCollisionTable) {
            this.persistCollisionTable = persistCollisionTable;
            return this;
        }

        public Builder setWebG(boolean webGraph){
            this.webG = webGraph;
            return this;
        }

        public Builder setCompG(boolean compG){
            this.compG = compG;
            return this;
        }

        public Builder setECompG(boolean compEG){
            this.compEGraph = compEG;
            return this;
        }

        public Builder setDifferentialCompression(boolean diffC){
            this.differentialCompression = diffC;
            return this;
        }

        public Parameter build() {
            return new Parameter(this);
        }
    }
}
