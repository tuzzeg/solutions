CREATE FUNCTION dbo.Similarity (@S1 varchar(255),@S2 varchar(255))  
RETURNS float AS  
BEGIN 
declare	@S3 varchar(255)
declare @i int
declare @ch char(1)
declare @p1 int
declare @sim1 float
declare @sim2 float
declare @R float
declare @h int
set @S3= @S2;
set @i=1;
set @h=0
while (@i <=Len(@S1))
begin
  set @ch = substring(@S1,@i,1);
  set @p1 = charindex(@ch, @S3);
  if @p1<>0	
     set @S3 = substring(@S3, 1, @p1 - 1) +substring(@S3,@p1 + 1, Len(@S3))
  else
     set @h=@h+1
  set @i=@i+1;
end;
set @sim1 = 1 - ((len(@s3)+0.0) / len(@s2))-((@h+0.0)/len(@s1));
if charindex(ltrim(rtrim(@S1)),ltrim(rtrim(@s2)))<>0
   set  @sim1=@sim1+0.3
set  @S3= @S1;
set @i=1;
set @h=0;
while ( @i<=Len(@S2))
begin
  set @ch = substring(@S2,@i,1);
  set @p1 = charindex(@ch, @S3);
  if @p1<>0	
     set @S3 = substring(@S3, 1, @p1 - 1) +substring(@S3,@p1 + 1, Len(@S3))
  else
     set @h=@h+1
  set @i=@i+1;
end;
set @sim2 = 1 - ((len(@s3)+0.0) / len(@s1))-((@h+1)/len(@s2));
if charindex(ltrim(rtrim(@S2)),ltrim(rtrim(@s1)))<>0
   set  @sim2=@sim2+0.3
set @R = (@sim1 + @sim2) / 2;
return @R
END









