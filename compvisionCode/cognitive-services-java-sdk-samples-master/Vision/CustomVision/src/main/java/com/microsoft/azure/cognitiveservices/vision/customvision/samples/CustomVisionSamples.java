/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.cognitiveservices.vision.customvision.samples;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.google.common.io.ByteStreams;

import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Classifier;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Domain;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.DomainType;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.ImageFileCreateBatch;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.ImageFileCreateEntry;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Iteration;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Project;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Region;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.TrainingApi;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.Trainings;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.CustomVisionTrainingManager;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.ImagePrediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.Prediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.PredictionEndpoint;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.CustomVisionPredictionManager;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Tag;


public class CustomVisionSamples {
    /**
     * Main entry point.
     * @param trainer the Custom Vision Training client object
     * @param predictor the Custom Vision Prediction client object
     */
    public static void runSample(TrainingApi trainer, PredictionEndpoint predictor) {
        try {
            // This demonstrates how to create an image classification project, upload images,
            // train it and make a prediction.
            ImageClassification_Sample(trainer, predictor);

            // This demonstrates how to create an object detection project, upload images,
            // train it and make a prediction.
//            ObjectDetection_Sample(trainer, predictor);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ImageClassification_Sample(TrainingApi trainClient, PredictionEndpoint predictor) {
        try {
            System.out.println("ImageClassification Sample");
            Trainings trainer = trainClient.trainings();

            System.out.println("Creating project...");
            Project project = trainer.createProject()
                .withName("Sample Java Project")
                .execute();

            // create stop tag
            Tag stopTag = trainer.createTag()
                .withProjectId(project.id())
                .withName("Stop")
                .execute();
            // create go tag
            Tag goTag = trainer.createTag()
                .withProjectId(project.id())
                .withName("Go")
                .execute();

//            Tag nTag = trainer.createTag()
//                    .withProjectId(project.id())
//                    .withName(" ")
//                    .execute();

            System.out.println("Adding images...");
            for (int i = 1; i <= 53; i++) {
                String fileName = "stop_" + i + ".jpg";
                byte[] contents = GetImage("/Stop", fileName);
                AddImageToProject(trainer, project, fileName, contents, stopTag.id(), null);
            }

            for (int i = 1; i <= 66; i++) {
                String fileName = "go_" + i + ".jpg";
                byte[] contents = GetImage("/Go", fileName);
                AddImageToProject(trainer, project, fileName, contents, goTag.id(), null);
            }

//            for (int i = 1; i <= 56; i++) {
//                String fileName = "n_" + i + ".jpg";
//                byte[] contents = GetImage("/n", fileName);
//                AddImageToProject(trainer, project, fileName, contents, nTag.id(), null);
//            }

            System.out.println("Training...");
            Iteration iteration = trainer.trainProject(project.id());

            while (iteration.status().equals("Training"))
            {
                System.out.println("Training Status: "+ iteration.status());
                Thread.sleep(1000);
                iteration = trainer.getIteration(project.id(), iteration.id());
            }
            System.out.println("Training Status: "+ iteration.status());
            trainer.updateIteration(project.id(), iteration.id(), iteration.withIsDefault(true));

            // use below for url
            // String url = "some url";
            // ImagePrediction results = predictor.predictions().predictImage()
            //                         .withProjectId(project.id())
            //                         .withUrl(url)
            //                         .execute();

            // load test image
            byte[] testImage = GetImage("/Test", "test_stop.jpg");

            // predict
            ImagePrediction results = predictor.predictions().predictImage()
                .withProjectId(project.id())
                .withImageData(testImage)
                .execute();

            for (Prediction prediction: results.predictions())
            {
                System.out.println(String.format("\t%s: %.2f%%", prediction.tagName(), prediction.probability() * 100.0f));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    private static void AddImageToProject(Trainings trainer, Project project, String fileName, byte[] contents, UUID tag, double[] regionValues)
    {
        System.out.println("Adding image: " + fileName);
        ImageFileCreateEntry file = new ImageFileCreateEntry()
            .withName(fileName)
            .withContents(contents);

        ImageFileCreateBatch batch = new ImageFileCreateBatch()
            .withImages(Collections.singletonList(file));

        // If Optional region is specified, tack it on and place the tag there, otherwise
        // add it to the batch.
        if (regionValues != null)
        {
            Region region = new Region()
                .withTagId(tag)
                .withLeft(regionValues[0])
                .withTop(regionValues[1])
                .withWidth(regionValues[2])
                .withHeight(regionValues[3]);
            file = file.withRegions(Collections.singletonList(region));
        } else {
            batch = batch.withTagIds(Collections.singletonList(tag));
        }

        trainer.createImagesFromFiles(project.id(), batch);
    }

    private static byte[] GetImage(String folder, String fileName)
    {
        try {
            return ByteStreams.toByteArray(CustomVisionSamples.class.getResourceAsStream(folder + "/" + fileName));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Main entry point.
     *
     * @param args the parameters
     */
    public static void main(String[] args) {
        try {
            //=============================================================
            // Authenticate

            final String trainingApiKey = System.getenv("AZURE_CUSTOMVISION_TRAINING_API_KEY");;
            final String predictionApiKey = System.getenv("AZURE_CUSTOMVISION_PREDICTION_API_KEY");;

            TrainingApi trainClient = CustomVisionTrainingManager.authenticate(trainingApiKey);
            PredictionEndpoint predictClient = CustomVisionPredictionManager.authenticate(predictionApiKey);

            runSample(trainClient, predictClient);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
