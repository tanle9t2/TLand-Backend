import os


from dotenv import load_dotenv


from sqlalchemy import create_engine, text

# Load environment variables
load_dotenv()


def fetch_mysql_data():
    engine = create_engine(os.getenv("MYSQL_URI"))
    with engine.connect() as conn:
        query = text("""
            SELECT 
                CONCAT(u.first_name,' ', u.last_name) AS teacher_name,
                c.id AS course_id,
                c.name AS course_name,
                c.description,
                cat.name AS category,
                c.price as course_price,
                s.id AS section_id,
                s.name AS section_name,
                ct.id AS content_id,
                ct.name AS content_name
            FROM course c
            LEFT JOIN user u ON u.id = c.teacher_id
            LEFT JOIN category cat ON c.category_id = cat.id
            LEFT JOIN section s ON s.course_id = c.id
            LEFT JOIN content ct ON ct.section_id = s.id
            WHERE c.description IS NOT NULL AND c.description != ''
        """)
        result = conn.execute(query)
        return [
            {
                "course": row.course_name,
                "section": row.section_name,
                "content_name": row.content_name,
                "teacher": row.teacher_name,
                "category": row.category,
                "description": row.description,
                "price": int(row.course_price),
            }
            for row in result.fetchall()
        ]




if __name__ == "__main__":
    index_to_pinecone()
