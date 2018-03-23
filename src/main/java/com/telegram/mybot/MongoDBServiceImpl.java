package com.telegram.mybot;

import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.telegram.mybot.model.UserData;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.security.NoSuchAlgorithmException;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBServiceImpl implements MongoDBServiceApi {

    private Gson gson = new Gson();

    private MongoClient mongoClient;

    public void init() {
        mongoClient = new MongoClient("localhost", 27017);
        MongoCollection<Document> collection = database().getCollection("usersData");
        collection.createIndex(new Document("userHashId", 1));
    }

    private MongoDatabase database() {
        return mongoClient.getDatabase("usersDataDatabase");
    }

    @Override
    public UserData getUserData(Long userId) {
        MongoCollection<Document> collection = database().getCollection("usersData");
        try {
            return gson.fromJson(JSON.serialize(collection.find(eq("userHashId", userId)).first().get("user")), UserData.class);
        } catch (Exception e) {
            System.out.println("Error during to get data");
        }

        return null;
    }

    public void putUserData(UserData userData) throws NoSuchAlgorithmException {
        MongoCollection<Document> collection = database().getCollection("usersData");
        Bson bson = BsonDocument.parse(gson.toJson(userData));
        Document document = new Document();
        document.put("user", bson);
        document.put("userHashId", userData.getUserId());
        try {
            if (collection.find(eq("userHashId", userData.getUserId())).first() == null ) {
                collection.insertOne(document);
            } else {
                collection.findOneAndReplace(eq("userHashId", userData.getUserId()), document);
            }
        } catch (Exception e) {
            System.out.println("Error during to put data");
        }
    }

    public void updateHistory(UserData userData) throws NoSuchAlgorithmException {
       putUserData( userData );
    }

    public void removeCategory(String categoryName) {
    }

    public void removeData( UserData userData ) throws NoSuchAlgorithmException {
        putUserData( userData );
    }

    public void cleanDatabase() {
        mongoClient.getDatabase("usersDataDatabase").drop();
    }
}
