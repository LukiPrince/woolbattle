/**
 MIT License

 Copyright (c) 2022-present SimsumMC, Servaturus and Flashtube

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package woolbattle.woolbattle.maprestaurationsystem;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.World;
import woolbattle.woolbattle.MapConfig;
import woolbattle.woolbattle.Main;
import java.util.ArrayList;


public class MapSystem {

    private static String mapChunksId() { return "mapChunks_" + MapConfig.mapName; }

    public static void defineMapChunks(ArrayList<ArrayList<Long>> chunks){
        String id = mapChunksId();

        if(Main.getStore().find("map", id) == null){
            Main.getStore().insert("map", new Document("_id", id).append("chunks", new ArrayList<ArrayList<Long>>()));
        }

        ArrayList<ArrayList<Long>> dbChunks;
        try{
            dbChunks = (ArrayList<ArrayList<Long>>) Main.getStore().find("map", id).get("chunks");
        }catch(ClassCastException e){
            Main.getInstance().getLogger().warning("The chunks value in the database could not be cast to an ArrayList.");
            dbChunks = new ArrayList<>();
        }

        if(dbChunks != null && dbChunks.size() >0 ){
            for(ArrayList<Long> chunk : dbChunks){
                if (!chunks.contains(chunk)) {
                    chunks.add(chunk);
                }
            }
        }

        Document chunkDoc = new Document("_id", id).append("chunks", chunks);
        Main.getStore().replace("map", id, chunkDoc);
    }

    public static ArrayList<ArrayList<Long>> getChunksInRange(World world /*temporary undefined parameter, to be modified in the future*/, long bx, long by, long ex, long ey){

        boolean bxGreaterEqualsEx= bx>=ex;
        boolean byGreaterEqualsEy = by>=ey;
        ArrayList<ArrayList<Long>> result = new ArrayList<>();
        for(long i = (bxGreaterEqualsEx)?  ex : bx; (bxGreaterEqualsEx)? i<bx : i<ex; i++){
            for(long j = (byGreaterEqualsEy)? ey : by; (byGreaterEqualsEy)? j<by : j<ey; j++){
                ArrayList<Long> iterChunk = new ArrayList<>();
                iterChunk.add(i);
                iterChunk.add(j);
                result.add(iterChunk);
            }
        }
        return result;
    }

    public static void clearMapChunks(){

    }
}