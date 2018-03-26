package com.telegram.mybot;

import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.telegram.mybot.model.Category;
import com.telegram.mybot.model.UserData;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

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
    public UserData getUserData(Message message) {
        MongoCollection<Document> collection = database().getCollection("usersData");
        try {
            return gson.fromJson(JSON.serialize(collection.find(eq("userHashId", message.getChatId())).first().get("user")), UserData.class);
        } catch (Exception e) {
            System.out.println("Error during to get data");
            UserData userData = createBaseUser(message);
            try {
                putUserData(userData);
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
            return userData;
        }
    }

    public void putUserData(UserData userData) throws NoSuchAlgorithmException {
        MongoCollection<Document> collection = database().getCollection("usersData");
        Bson bson = BsonDocument.parse(gson.toJson(userData));
        Document document = new Document();
        document.put("user", bson);
        document.put("userHashId", userData.getUserId());
        try {
            if (collection.find(eq("userHashId", userData.getUserId())).first() == null) {
                collection.insertOne(document);
            } else {
                collection.findOneAndReplace(eq("userHashId", userData.getUserId()), document);
            }
        } catch (Exception e) {
            System.out.println("Error during to put data");
        }
    }

    public void updateHistory(UserData userData) throws NoSuchAlgorithmException {
        putUserData(userData);
    }

    public void removeCategory(UserData userData, String categoryName) throws NoSuchAlgorithmException {
        Integer removingRootCategoryIndex = null;
        for (int j = 0; j < userData.getCategories().size(); j++) {
            if (categoryName.equals(userData.getCategories().get(j).getName())) {
                removingRootCategoryIndex = j;
                break;
            }
            Integer removingChildCategoryIndex = null;
            for (int i = 0; i < userData.getCategories().get(j).getCategories().size(); i++) {
                if (categoryName.equals(userData.getCategories().get(j).getCategories().get(i).getName())) {
                    removingChildCategoryIndex = i;
                }
            }
            if (removingChildCategoryIndex != null) {
                userData.getCategories().get(j).getCategories().remove(removingChildCategoryIndex.intValue());
            }
        }
        if ( removingRootCategoryIndex != null ) {
            userData.getCategories().remove( removingRootCategoryIndex.intValue() );
        }
        putUserData(userData);
    }

    public void removeData(UserData userData) throws NoSuchAlgorithmException {
        putUserData(userData);
    }

    public void cleanDatabase() {
        mongoClient.getDatabase("usersDataDatabase").drop();
    }

    private UserData createBaseUser(Message message) {
        UserData userData = new UserData(null);
        Optional.ofNullable(message.getFrom()).map(User::getFirstName).ifPresent(userData::setUserFirstName);
        Optional.ofNullable(message.getFrom()).map(User::getLastName).ifPresent(userData::setUserLastName);
        Optional.ofNullable(message.getChatId()).ifPresent(userData::setUserId);
        return userData;
    }
}
