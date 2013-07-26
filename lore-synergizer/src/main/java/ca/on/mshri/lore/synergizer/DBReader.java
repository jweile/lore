/*
 *  Copyright (C) 2011 The Roth Lab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.on.mshri.lore.synergizer;

import de.jweile.yogiutil.CliProgressBar;
import de.jweile.yogiutil.pipeline.StartNode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pipeline start node that reads from the database
 * 
 * @author Jochen Weile <jochenweile@gmail.com>
 */
final class DBReader extends StartNode<Entry> {
    
    /**
     * DB connection
     */
    private Connection db;
    
    /**
     * username and password
     */
    private String user, pwd;
    
    /**
     * array of gene IDs, over which to iterate
     */
    private int[] geneIds;
    
    /**
     * current geneId
     */
    private int geneIdPointer = 0;
    
    /**
     * progress bar
     */
    private CliProgressBar pb;

    /**
     * constructor
     * @param user user name
     * @param pwd password
     */
    public DBReader(String user, String pwd) {
        super("DB Reader");
        this.user = user;
        this.pwd = pwd;
    }

    
    /**
     * set up db connection and list of genes
     */
    @Override
    public void before() {
        try {
            
            //load driver
            Class.forName("org.postgresql.Driver");
            
            //open connection
            db = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/synergizer", 
                    user, pwd);
            
            
            //query gene ids and store in array
            //first get array size and init it.
            Statement s = db.createStatement();
            ResultSet r = s.executeQuery("SELECT COUNT(id) FROM gene;");
            r.next();
            geneIds = new int[r.getInt(1)];
            s.close();
            
            //then fill it with contents.
            s = db.createStatement();
            r = s.executeQuery("SELECT id FROM gene;");
            int i=0;
            while (r.next()) {
                geneIds[i++] = r.getInt(1);
            }
            s.close();
            
            //init progressbar
            pb = new CliProgressBar(geneIds.length);
            
        } catch (SQLException e) {
            throw new RuntimeException("Initiation of DB connection failed!", e);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Failed to invoke JDBC driver. Report this as a bug!");
        }
            
    }
    
    /**
     * closes the db connection
     */
    @Override
    public void after() {
        try {
            db.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBReader.class.getName())
                    .log(Level.SEVERE, "Failed to close DB connection!", ex);
        }
    }
    

    /**
     * generate an entry
     */
    @Override
    public Entry process(Void in) {
        
        //termination case
        if (geneIdPointer >= geneIds.length) {
            Logger.getLogger(DBReader.class.getName()).info("Finished reading DB!");
            return null;
        }
        
        //get current gene id.
        int currGeneId = geneIds[geneIdPointer++];
        
        try {
               
            //get species and source of gene to create entry
            Statement s = db.createStatement();
            ResultSet r = s.executeQuery(
                    "SELECT org_id, source_id FROM gene WHERE id='"+
                    currGeneId+"';");
            r.next();
            Entry e = new Entry(currGeneId, 
                    r.getInt("org_id"), 
                    r.getInt("source_id")
                    );
            s.close();
            
            //get all synonyms for gene
            s = db.createStatement();
            r = s.executeQuery(
                    "SELECT id, source_id, ns_id FROM synonym WHERE gene_id='"+
                    currGeneId+"';"
                    );
            
            //add synonyms to entry
            while (r.next()) {
                e.addSynonym(new Entry.Synonym(r.getString("id"), 
                        r.getInt("source_id"), 
                        r.getInt("ns_id")
                        ));
            }
            s.close();
            
            //update progressbar
            pb.next();
            
            //return entry.
            return e;
            
        } catch (SQLException ex) {
            throw new RuntimeException("Query failed!",ex);
        }
    }
    
    
}
