package com.project.dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.project.datasource.DataSource;
import com.project.model.Projekt;

public class ProjektDAOImpl implements ProjektDAO {

	@Override
	public Projekt getProjekt(Integer projektId) {
		String query = "SELECT * FROM projekt WHERE projektId = ?";
		try (Connection connect  = DataSource.getConnection();
				PreparedStatement preparedStmt = connect.prepareStatement(query)) {
			preparedStmt.setInt(1, projektId);
		
		try (ResultSet rs = preparedStmt.executeQuery()) {
			if (rs.next()) {
			 Projekt projekt = new Projekt();
			 projekt.setProjektId(rs.getInt("projekt_id"));
			 projekt.setNazwa(rs.getString("nazwa"));
			 projekt.setOpis(rs.getString("opis"));
			 projekt.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
			 projekt.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
			 return projekt;
			} else {
				return null;
			}
		}
	} catch (SQLException e) {
		throw new RuntimeException(e);
		}
	}

	@Override
	public void setProjekt(Projekt projekt) {
		boolean isInsert = projekt.getProjektId() == null;
		String query = isInsert ?
				"INSERT INTO projekt(nazwa, opis, dataczas_utworzenia, data_oddania) VALUES (?,?,?,?)" :
				"UPDATE projekt SET nazwa = ?, opis = ?, dataczas_utworzenia = ?, data_oddania = ?"
					+ " WHERE projekt_id = ?";
		try (Connection connect = DataSource.getConnection();
			PreparedStatement prepStmt = connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			prepStmt.setString(1,  projekt.getNazwa());
			prepStmt.setString(2, projekt.getOpis());
			if(projekt.getDataCzasUtworzenia() == null)
				projekt.setDataCzasUtworzenia(LocalDateTime.now());
			prepStmt.setObject(3, projekt.getDataCzasUtworzenia());
			prepStmt.setObject(4, projekt.getDataOddania());
			if(!isInsert) {
				ResultSet keys = prepStmt.getGeneratedKeys();
			keys.close();
			}
		}catch(SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteProjekt(Integer projektId) {
		String query = "DELETE FROM projekt WHERE projekt_id = ?";
		try (Connection connect = DataSource.getConnection();
				PreparedStatement prepStmt = connect.prepareStatement(query)) {
			prepStmt.setInt(1, projektId);
		}catch(SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Projekt> getProjekty(Integer offset, Integer limit) {
		 List<Projekt> projekty = new ArrayList<>();
		 String query = "SELECT * FROM projekt ORDER BY dataczas_utworzenia DESC"
		 + (offset != null ? " OFFSET ?" : "")
		 + (limit != null ? " LIMIT ?" : "");
		 try (Connection connect = DataSource.getConnection();
		 PreparedStatement preparedStmt = connect.prepareStatement(query)) {
		 int i = 1;
		 if (offset != null) {
		 preparedStmt.setInt(i, offset);
		 i += 1;
		 }
		 if (limit != null) {
		 preparedStmt.setInt(i, limit);
		 }
		 try (ResultSet rs = preparedStmt.executeQuery()) {
		 while (rs.next()) {
		 Projekt projekt = new Projekt();
		 projekt.setProjektId(rs.getInt("projekt_id"));
		 projekt.setNazwa(rs.getString("nazwa"));
		 projekt.setOpis(rs.getString("opis"));
		 projekt.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
		 projekt.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
		 projekty.add(projekt);
		 }
		 }
		 }catch(SQLException e) {
		 throw new RuntimeException(e);
		 }
		 return projekty;
		} 

	@Override
	public List<Projekt> getProjektyWhereNazwaLike(String nazwa, Integer offset, Integer limit) {
	    List<Projekt> projekty = new ArrayList<>();
	    String query = "SELECT * FROM projekt WHERE nazwa LIKE ?" 
	        + (offset != null ? " OFFSET ?" : "")
	        + (limit != null ? " LIMIT ?" : "");

	    try (Connection connect = DataSource.getConnection();
	         PreparedStatement preparedStmt = connect.prepareStatement(query)) {
	        
	        int i = 1;
	        // Ustawienie parametru dla warunku WHERE
	        preparedStmt.setString(i, "%" + nazwa + "%");
	        i += 1;

	        // Ustawienie offsetu, jeśli jest podany
	        if (offset != null) {
	            preparedStmt.setInt(i, offset);
	            i += 1;
	        }

	        // Ustawienie limitu, jeśli jest podany
	        if (limit != null) {
	            preparedStmt.setInt(i, limit);
	        }

	        // Wykonanie zapytania i przetwarzanie wyników
	        try (ResultSet rs = preparedStmt.executeQuery()) {
	            while (rs.next()) {
	                Projekt projekt = new Projekt();
	                projekt.setProjektId(rs.getInt("projekt_id"));
	                projekt.setNazwa(rs.getString("nazwa"));
	                projekt.setOpis(rs.getString("opis"));
	                projekt.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
	                projekt.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
	                projekty.add(projekt);
	            }
	        }
	    } catch (SQLException e) {
	        throw new RuntimeException(e);
	    }
	    return projekty;
	}

	@Override
	public List<Projekt> getProjektyWhereDataOddaniaIs(LocalDate dataOddania, Integer offset, Integer limit) {
	    List<Projekt> projekty = new ArrayList<>();
	    String query = "SELECT * FROM projekt WHERE data_oddania = ?"
	        + (offset != null ? " OFFSET ?" : "")
	        + (limit != null ? " LIMIT ?" : "");

	    try (Connection connect = DataSource.getConnection();
	         PreparedStatement preparedStmt = connect.prepareStatement(query)) {

	        int i = 1;
	        // Ustawienie parametru dla warunku WHERE
	        preparedStmt.setDate(i, java.sql.Date.valueOf(dataOddania));
	        i += 1;

	        // Ustawienie offsetu, jeśli jest podany
	        if (offset != null) {
	            preparedStmt.setInt(i, offset);
	            i += 1;
	        }

	        // Ustawienie limitu, jeśli jest podany
	        if (limit != null) {
	            preparedStmt.setInt(i, limit);
	        }

	        // Wykonanie zapytania i przetwarzanie wyników
	        try (ResultSet rs = preparedStmt.executeQuery()) {
	            while (rs.next()) {
	                Projekt projekt = new Projekt();
	                projekt.setProjektId(rs.getInt("projekt_id"));
	                projekt.setNazwa(rs.getString("nazwa"));
	                projekt.setOpis(rs.getString("opis"));
	                projekt.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
	                projekt.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
	                projekty.add(projekt);
	            }
	        }
	    } catch (SQLException e) {
	        throw new RuntimeException(e);
	    }

	    return projekty;
	}

	@Override
	public int getRowsNumber() {
	    String query = "SELECT COUNT(*) FROM projekt";

	    try (Connection connect = DataSource.getConnection();
	         PreparedStatement preparedStmt = connect.prepareStatement(query);
	         ResultSet rs = preparedStmt.executeQuery()) {

	        if (rs.next()) {
	            return rs.getInt(1);
	        }
	    } catch (SQLException e) {
	        throw new RuntimeException(e);
	    }

	    return 0;
	}

	@Override
	public int getRowsNumberWhereNazwaLike(String nazwa) {
	    String query = "SELECT COUNT(*) FROM projekt WHERE nazwa LIKE ?";

	    try (Connection connect = DataSource.getConnection();
	         PreparedStatement preparedStmt = connect.prepareStatement(query)) {

	        preparedStmt.setString(1, "%" + nazwa + "%");

	        try (ResultSet rs = preparedStmt.executeQuery()) {
	            if (rs.next()) {
	                return rs.getInt(1);
	            }
	        }
	    } catch (SQLException e) {
	        throw new RuntimeException(e);
	    }

	    return 0;
	}

	@Override
	public int getRowsNumberWhereDataOddaniaIs(LocalDate dataOddania) {
	    String query = "SELECT COUNT(*) FROM projekt WHERE data_oddania = ?";

	    try (Connection connect = DataSource.getConnection();
	         PreparedStatement preparedStmt = connect.prepareStatement(query)) {

	        preparedStmt.setDate(1, java.sql.Date.valueOf(dataOddania));

	        try (ResultSet rs = preparedStmt.executeQuery()) {
	            if (rs.next()) {
	                return rs.getInt(1);
	            }
	        }
	    } catch (SQLException e) {
	        throw new RuntimeException(e);
	    }

	    return 0;
	}

}
